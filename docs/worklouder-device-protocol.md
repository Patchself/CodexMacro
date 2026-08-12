# Work Louder / OpenAI 设备通信协议分析

> 分析日期：2026-07-22
> 来源：`/Applications/ChatGPT.app/Contents/Resources/app.asar`
> ChatGPT Desktop：`26.715.70719`（build `5650`）
> `app.asar` SHA-256：`954760af20a1b74275a9db50c99a09266da4f5d1e08f4b613c8a46f97adc9ce4`

## 1. 分析范围与版本

本文件根据 ChatGPT Desktop 随包 JavaScript 和 TypeScript 声明整理，不是 Work Louder 官方协议规范。

| 包 | 版本 | ASAR 内实际位置 |
|---|---:|---|
| `@worklouder/device-kit-oai` | `0.1.10` | `node_modules/@worklouder/device-kit-oai` |
| `@worklouder/wl-device-kit` | `0.1.18` | `node_modules/@worklouder/device-kit-oai/node_modules/@worklouder/wl-device-kit` |

注意：当前归档中 `wl-device-kit` 不是顶层 `node_modules/@worklouder/wl-device-kit`，而是 `device-kit-oai` 的嵌套依赖。

分析覆盖：

- USB HID 发现与分帧；
- 运行时串口通信；
- JSON-RPC 风格请求、响应和通知；
- 旧式 `#rpc#...#` 文本协议；
- 通用 Work Louder RPC；
- OpenAI 专用灯光、按键和摇杆 RPC；
- ChatGPT Desktop 的 thread slot 分配、状态映射与按键行为；
- 与当前 CodexMacro 实现直接相关的兼容性差异。

固件烧录由 `esptool-js` 实现，属于 Espressif Boot ROM / SLIP 协议，不是这两个包自行定义的运行时 RPC，本文不展开其底层字节协议。

## 2. 协议栈概览

```text
RPCApiOAI
  ├─ OpenAI vendor RPC: v.oai.*
  └─ WLRPCApi
       ├─ sys.*, device.*, fs.*, mp.*, ui.*, lights.*, host.*, wlsdk.*
       └─ WLRPCClient
            ├─ JSON object serialization + non-ASCII Unicode escaping
            └─ WLDeviceCommImpl
                 ├─ USB HID: 64-byte reports, report ID 6
                 └─ Serial: 115200 baud, raw UTF-8 stream
```

最重要的结论：包内虽将其称为“JSON-RPC 2.0”，但实际请求没有 `jsonrpc: "2.0"` 字段。兼容实现应复制这里的实际报文，不应擅自补齐严格 JSON-RPC 2.0 信封。

## 3. 设备发现

### 3.1 USB HID 过滤条件

| 字段 | 值 |
|---|---:|
| USB VID | `12346` / `0x303A` |
| HID Usage Page | `65280` / `0xFF00` |
| Manufacturer | 包含 `Work Louder` 或 `Work_Louder`；匹配不到时退化为仅按 VID 过滤 |

已登记 PID：

| PID（十进制） | PID（十六进制） | 设备类型 | 布局 |
|---:|---:|---|---|
| `4097` | `0x1001` | `nomad_e` | `unknown` |
| `33428` | `0x8294` | `nomad_e` | `ansi` |
| `33429` | `0x8295` | `nomad_e` | `iso` |
| `33430` | `0x8296` | `knob` | `ansi` |
| `33507` | `0x82E3` | `knob` | `iso` |
| `33431` | `0x8297` | `creator_micro_v2` | `universal` |
| `33432` | `0x8298` | `creator_micro_v2` | `universal` |
| `33632` | `0x8360` | `project_2077` | `universal` |
| `33606` | `0x8346` | `xyz` | `ansi` |

ChatGPT Desktop 的 Codex Micro 服务进一步固定匹配 `VID 0x303A + PID 0x8360 + Usage Page 0xFF00`。

### 3.2 连接类型

```text
ConnectionType.serial = 0
ConnectionType.hid    = 1
```

运行中设备主要使用 HID。Bootloader 设备通过串口发现：VID 必须为 `0x303A`，非 Windows 平台要求 manufacturer 等于 `Espressif`，Windows 还支持 serial number 以 `0000` 结尾的设备。

## 4. USB HID 传输层

### 4.1 64 字节报告格式

```text
Offset  Size  Name             Value / Meaning
0       1     report_id        0x06
1       1     channel          0x01 = debug log, 0x02 = RPC
2       1     payload_length   0..61
3       61    payload          UTF-8 bytes, unused tail is zero-filled
```

- 主机发出的 RPC 始终使用 channel `2`。
- 单帧最多承载 `61` 字节；长消息按 UTF-8 字节流连续切片。
- 包内没有序号、总长度、结束位、校验和或重传机制。
- 分帧顺序依赖 HID write 顺序；消息边界依赖上层 JSON 可解析性或设备回包中的换行符。
- `payload_length` 是字节数，不是 Unicode 字符数。

例如请求：

```json
{"method":"sys.version","params":null,"id":42}
```

共 46 字节，只需一帧：

```text
06 02 2e 7b 22 6d 65 74 68 6f 64 22 3a 22 73 79
73 2e 76 65 72 73 69 6f 6e 22 2c 22 70 61 72 61
6d 73 22 3a 6e 75 6c 6c 2c 22 69 64 22 3a 34 32
7d 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
```

### 4.2 HID 接收与重组

接收端按 channel 分别维护字符串缓冲区：

- channel `1`：按行输出调试日志；
- channel `2`：按 `\n` 或 `\r\n` 切行，再交给 RPC JSON 解析器；
- 解析单帧时直接读取 `[channel, length, payload]`，没有验证 report ID、channel 范围或 length 是否超过 61；
- 某帧解析异常时会同时清空 debug 和 RPC 两个 channel 的行缓冲。

因此，设备回给 ChatGPT Desktop 的 HID RPC 响应和通知应以换行结束。CodexMacro 的 `CodexProtocol.frame()` 在 JSON 后追加 `\n` 是正确的设备侧行为。

### 4.3 BLE HID 与 USB HID 的首字节差异

CodexMacro 使用 BLE HID Report characteristic。GATT Report characteristic 的 value 通常不携带 Report ID，因为 Report ID 由 Report Reference Descriptor 表达。因此：

```text
USB/node-hid view: [0x06, channel, length, payload...]
BLE GATT value:    [channel, length, payload...]
```

当前 CodexMacro 的 63 字节 report body 与外部包的 64 字节 node-hid report 在语义上是一致的。

### 4.4 CodexMacro 自定义键盘层扩展

CodexMacro 在同一 HID service 中额外暴露标准 Keyboard Usage Page `0x07` 的输入 Report：

| Report ID | 类型 | 长度 | 用途 |
|---:|---|---:|---|
| `1` | Input | 8 bytes | modifiers、reserved、6-key rollover usages |
| `6` | Input / Output | 63 bytes | Work Louder OAI 厂商 RPC |

第 1 层只发送 Report ID `6` 的 Codex 输入；第 2 至 6 层的 12 个可编辑按键只发送 Report ID
`1` 的标准键盘按下/松开。这个键盘 Report 是 CodexMacro 的应用扩展，不是 Work Louder OAI
SDK 中发现的 layer schema。HID Report Map 发生变化后，已配对主机需要忽略设备并重新配对。

## 5. 串口传输层

运行时串口参数：

| 参数 | 值 |
|---|---:|
| Baud rate | `115200` |
| 写入 | 原始 UTF-8 字符串，随后 drain |
| JSON 接收 | 原始 data chunk 累积，直到 `JSON.parse` 成功 |
| Legacy 接收 | 另接 `\n` delimiter parser，逐行解析 |

串口 JSON 请求不会由包自动追加换行。Firmware flash 默认 `921600` baud，但那是 `esptool-js` 烧录链路，不应与运行时 RPC 的 `115200` 混用。

## 6. RPC 信封

### 6.1 请求

实际请求结构：

```ts
interface Request {
  method: string
  params: unknown | null
  id: number
}
```

示例：

```json
{"method":"device.status","params":null,"id":317}
```

行为约束：

- 自动生成的 `id` 为 `0..998` 的随机整数，即 `randomInt(0, 999)`；
- transport 内部把 `id` 转成字符串作为 resolver key；
- `params` 省略时，客户端实际序列化为 `null`；
- 所有非 ASCII 字符都会转换成小写十六进制 `\uXXXX`；非 BMP 字符转换成一对 UTF-16 surrogate escape；
- 不包含 `jsonrpc` 字段。

### 6.2 成功响应

```json
{"result":{"version":"1.2.3"},"id":317}
```

客户端使用 `id` 匹配请求，只解析完整字段 `result`。

### 6.3 错误响应

```json
{
  "error": {
    "code": -32601,
    "message": "Method not found",
    "data": null
  },
  "id": 317
}
```

上层只把 `error.message` 包装成普通 `Error` 抛出，`code` 和 `data` 不会保留在抛出的错误对象中。

### 6.4 设备主动通知

通知没有 `id`，支持完整键名：

```json
{"method":"v.oai.hid","params":{"k":"AG00","act":1,"ag":0}}
```

接收器也兼容紧凑键名：

```json
{"m":"v.oai.hid","p":{"k":"AG00","act":1,"ag":0}}
```

兼容情况：

| 语义 | 完整键 | 紧凑键 | 是否支持 |
|---|---|---|---|
| response id | `id` | `i` | 是 |
| notification method | `method` | `m` | 是 |
| notification params | `params` | `p` | 是 |
| success result | `result` | 未发现 | 仅完整键 |
| error | `error` | 未发现 | 仅完整键 |

### 6.5 串行队列、超时和取消

- 所有 legacy 与 JSON 请求进入同一个 FIFO 队列；
- 同一时刻只允许一个请求 in-flight，因此全局 JSON 累积缓冲不会混入多个响应；
- 每项结束后固定等待 `50 ms` 再处理下一项；
- 请求超时为 `10 s`；
- 取消排队请求时按字符串 id 从队列移除；该排队任务自身的 Promise 不会被显式 resolve/reject；
- 若请求已 in-flight，则本地注入 `{"error":{"message":"task aborted"}}` 结束 Promise，不会向设备发送取消报文；
- 已知 transport error code：`ALREADY_CONNECTED`、`TIMEOUT`、`WRITE_FAILED`、`DEVICE_DISCONNECTED`。

## 7. Legacy 文本协议

### 7.1 请求格式

无参数：

```text
#<rpc>#\r\n
```

有参数：

```text
#<rpc>#<args>#\r\n
```

### 7.2 响应格式

```text
#<rpc>#<payload>#\r\n
```

已实现的命令：

| 命令 | 响应处理 |
|---|---|
| `version` | 返回 payload 字符串 |
| `dfu` | payload 等于 `ok` 时返回 `true`；API 没有对应公开 wrapper |
| `selftest` | payload 等于 `ok` 时返回 `true` |
| `bootloader` | payload 等于 `ok` 时返回 `true` |

## 8. `wl-device-kit` 通用 RPC

下表中的“响应”是上层代码实际读取的结构；没有读取字段的命令只要求收到无 error 的匹配响应。

| Method | Params | 上层读取的 Result / 说明 |
|---|---|---|
| `sys.version` | `null` | `{ version: string }` |
| `device.status` | `null` | `{ version?, profile_index?, layer_index?, battery?, is_charging? }` |
| `sys.bootloader` | `null` | 任意 result；设备随后断开 |
| `sys.selftest` | `null` | 任意 result |
| `mp.write_info` | `{ song_title?, artist?, elapsed?, total_duration?, is_playing? }` | 仅检查无 error |
| `mp.write_artwork` | `{ data, offset, size }` | `data` 为 Base64，`offset`/`size` 为原始字节数 |
| `fs.list` | `{ checksum: true }` | `[{ name, size, checksum? }]` |
| `fs.read` | `{ file }` | 任意 JSON result |
| `fs.write` | `{ file, data }` | 任意 result |
| `fs.writebin` | `{ file, data, append: true, completed, offset }` | `data` 为 Base64 |
| `fs.readbin` | `{ file, offset, len }` | `{ total_size, data }`，`data` 为 Base64 |
| `fs.delete` | `{ file }` | 仅检查无 error |
| `ui.active_screen` | `null` | `{ screen_name: string }` |
| `ui.home_accent_color` | `{ color }` | `color` 可为 `"#FF5500"` 等 firmware 接受的字符串 |
| `lights.preview` | `{ backlight, underglow }` | 实时预览，不持久化 |
| `host.focused_app` | `{ appName?, process?, path? }` | 仅检查无 error |
| `wlsdk.<method>` | positional params array 或 `undefined` | 动态 custom widget RPC |

### 8.1 文件分块

- Base64 单块最大 `4096` 字符；
- 对应原始数据块固定为 `3072` 字节；
- `fs.writebin.offset` 按原始字节计算；
- `completed` 仅最后一块为 `true`；
- `mp.write_artwork.size` 是完整原始图片大小。

### 8.2 Work Louder Layers

这两个包中没有独立的 `Layer` interface、layer keymap schema 或 `layer.*` RPC。与 Work Louder Layers 直接相关的公开定义只有 `device.status` 返回的当前索引：

```json
{
  "id": 7,
  "result": {
    "version": "1.2.3",
    "profile_index": 0,
    "layer_index": 2,
    "battery": 86,
    "is_charging": false
  }
}
```

SDK 转换为：

```ts
type WLDeviceStatus = {
  firmwareVersion?: string
  selectedProfileIndex?: number
  selectedLayerIndex?: number
  batteryPercentage?: number
  isCharging?: boolean
}
```

`selectedProfileIndex` 和 `selectedLayerIndex` 都被声明为 zero-based；其中 layer index 是当前 profile 内的索引。

在本次分析范围内没有发现以下定义：

- layer 数量、名称或 ID；
- layer keymap / action 数据结构；
- 查询、切换或写入 layer 的专用 RPC；
- profile 与 layer 配置文件的文件名或 JSON schema。

虽然通用 `fs.read` / `fs.write` 理论上可以传输任意设备配置文件，但包没有把任何文件明确标注为 Layers 配置。因此不能仅凭这些包推断 Work Louder Layers 应用保存到设备的格式。

与 profile 切换间接相关的接口是：

```json
{
  "method": "host.focused_app",
  "params": {
    "appName": "Google Chrome",
    "process": "chrome",
    "path": "/Applications/Google Chrome.app"
  },
  "id": 9
}
```

`appName`、`process`、`path` 都是 optional string。SDK 注释说明 firmware 可根据前台应用显示 context-aware UI 或切换 profile，但请求中没有目标 profile / layer ID；具体匹配规则仍位于 firmware 或未包含在这些包里的设备配置中。

ChatGPT Desktop Codex Micro 服务不会调用 `host.focused_app`。它会调用 `device.status`，但只消费 `battery` 与 `is_charging`，不会根据 `profile_index` 或 `layer_index` 改变 task slot 或灯光。

### 8.3 `lights.preview`：通用全局灯光预览

通用 Work Louder API 定义两个 zone：

| Zone | SDK 描述 |
|---|---|
| `backlight` | per-key backlight LEDs |
| `underglow` | bottom-facing LED strip |

请求不会压缩字段名：

```json
{
  "method": "lights.preview",
  "params": {
    "backlight": {
      "effect": "solid",
      "brightness": 0.8,
      "speed": 0,
      "magic": 0,
      "color": 16733440
    },
    "underglow": {
      "effect": "rainbow",
      "brightness": 0.5,
      "speed": 0.3
    }
  },
  "id": 8
}
```

两个 zone 对象必填。每个 zone 只有 `effect` 必填，其余字段均可省略：

| Field | 类型 / 含义 |
|---|---|
| `effect` | string：`off / solid / snake / rainbow / breath / gradient` |
| `brightness` | optional number，约定 `0..1` |
| `speed` | optional number，约定 `0..1` |
| `magic` | optional number，effect-specific，约定 `0..1` |
| `color` | optional packed RGB integer |

SDK 明确说明这是实时 preview，立即生效但不持久化到 flash。`sendLightingPreview()` 捕获并记录 RPC 错误，返回类型为 `Promise<void>`，调用方不能从返回值区分成功或失败。

## 9. `device-kit-oai` 专用 RPC

### 9.1 Effect 编码

OpenAI vendor RPC 使用数字 enum：

| Code | Effect |
|---:|---|
| `0` | `off` |
| `1` | `solid` |
| `2` | `snake` |
| `3` | `rainbow` |
| `4` | `breath` |
| `5` | `gradient` |
| `6` | `shallowBreath`，亮度在约 0.5 到 1 之间呼吸 |

### 9.2 `v.oai.thstatus`：线程灯光

请求 params 是数组，每项只要求 `id`，其余字段省略表示保持设备当前值：

```json
{
  "method": "v.oai.thstatus",
  "params": [
    { "id": 0, "c": 16733440, "b": 1, "e": 4, "s": 0.4, "sk": 1, "sa": 0 },
    { "id": 1, "b": 0.5 }
  ],
  "id": 21
}
```

| Wire key | SDK field | 类型 / 含义 |
|---|---|---|
| `id` | `id` | number，线程槽位 |
| `c` | `color` | packed RGB integer |
| `b` | `brightness` | number，`0..1` |
| `e` | `effect` | 上表数字 enum |
| `s` | `speed` | number，`0..1` |
| `sk` | `syncKeysLighting` | `1` / `0`；SDK 由 boolean 转换 |
| `sa` | `syncAmbientLighting` | `1` / `0`；SDK 由 boolean 转换 |

SDK 没有删除值为 `undefined` 的属性；但 `JSON.stringify` 会自动省略这些对象属性，因此最终 wire payload 符合“缺省即不变”。

### 9.3 Slot 数量的协议边界

`device-kit-oai` 的 `ThreadLighting.id` 仅声明为 `number`，SDK 不校验范围，也没有定义最大 slot 数量。因此：

- **vendor RPC 层**：slot 是任意数字 ID，不能仅依据 SDK 推断固件上限；
- **当前 ChatGPT Desktop**：固定构造 6 个逻辑 slot，ID 为 `0..5`；
- **当前设备按键映射**：`AG00..AG05` 分别对应 slot `0..5`。

本文后续提到的“6 个 slot”均指当前 ChatGPT Desktop 行为，而非已证实的固件协议上限。

### 9.4 `v.oai.rgbcfg`：按键与环境灯

```json
{
  "method": "v.oai.rgbcfg",
  "params": {
    "ambient": { "e": 1, "b": 0.7, "s": 0, "m": 0, "c": 16733440 },
    "keys":    { "e": 0, "b": 0,   "s": 0, "m": 0, "c": 0 }
  },
  "id": 22
}
```

| Wire key | SDK field | 类型 / 含义 |
|---|---|---|
| `e` | `effect` | 数字 enum，必填 |
| `b` | `brightness` | number `0..1`，必填 |
| `s` | `speed` | number `0..1`，必填 |
| `m` | `magic` | effect-specific number，通常 `0..1`，必填 |
| `c` | `color` | packed RGB integer，必填 |

两个 zone `ambient` 与 `keys` 都是必填对象。

与 `lights.preview` 不同，每个 zone 的 `effect / brightness / speed / magic / color` 也全部必填，并使用数字 effect。SDK 只做字段改名，不做范围校验：

```text
ambient.effect     -> ambient.e
ambient.brightness -> ambient.b
ambient.speed      -> ambient.s
ambient.magic      -> ambient.m
ambient.color      -> ambient.c
```

`keys` 同样转换。两套全局灯光接口的名义对应关系如下，但包没有明确保证它们是完全相同的 firmware state：

| Generic API | OAI vendor API | 物理语义 |
|---|---|---|
| `backlight` | `keys` | 键帽下方 / per-key 灯光 |
| `underglow` | `ambient` | 底部灯带 / 外圈环境灯 |

当前 ChatGPT Desktop 只使用 `v.oai.rgbcfg`，没有调用 `lights.preview`。`device-kit-oai` 没有明确说明 `v.oai.rgbcfg` 是否持久化；Desktop 将它作为需要在连接、状态变化和退出时主动更新的 runtime state 使用。

### 9.5 `v.oai.hid`：按键通知

设备到主机的 notification：

```json
{"method":"v.oai.hid","params":{"k":"AG00","act":1,"ag":0}}
```

| Wire key | SDK field | 类型 / 含义 |
|---|---|---|
| `k` | `key` | string，HID key identifier |
| `act` | `act` | optional number，press/release 等动作 |
| `ag` | `agent` | optional number，触发事件的 agent index |

ChatGPT Desktop 当前用正则 `^AG0([0-5])$` 将 `AG00` 到 `AG05` 映射为 0 到 5 号 thread slot。包本身没有定义 `act` 的具体数值枚举。

### 9.6 `v.oai.rad`：摇杆通知

```json
{"method":"v.oai.rad","params":{"a":0.25,"d":0.8}}
```

| Wire key | SDK field | 类型 / 含义 |
|---|---|---|
| `a` | `angle` | number，声明约定为 `0..1` |
| `d` | `distance` | number，离中心距离 `0..1` |

ChatGPT Desktop 以 `distance > 0.1` 判定摇杆活动。包内没有说明 angle 的零点方向和顺/逆时针方向。

## 10. Thread slot 的应用层语义

本节来自 ChatGPT Desktop bundle，而不是 `device-kit-oai` 的公开类型声明。它解释应用如何把 Codex task 转换为 6 个设备 slot。

### 10.1 固定 slot 模型

应用始终生成 6 项：

```ts
type ThreadSlot = {
  id: 0 | 1 | 2 | 3 | 4 | 5
  threadKey: string | null
  title: string | null
  status:
    | "off"
    | "idle"
    | "working"
    | "unread"
    | "awaiting-approval"
    | "awaiting-response"
    | "error"
  selected: boolean
  pulsing?: boolean
}
```

没有 task 的位置仍保留 slot 项，但使用：

```json
{"id":3,"threadKey":null,"title":null,"status":"off","selected":false}
```

普通 task slot 的 `selected` 由 `threadKey === selectedThreadKey` 得出。如果 task 已被选中、主窗口处于 focus 状态，且原状态是 `unread`，应用会将设备显示状态降为 `idle`，因为用户已经在查看它。

### 10.2 Slot 数据源

设置项 `CODEX_MICRO_AGENT_SOURCE` 决定填入 slot 的 task 顺序：

| Mode | UI 名称 | 选择规则 |
|---|---|---|
| `pinned` | Pinned chats | pinned task 与 pinned project task 合并后取前 6 项 |
| `recent` | Most recent chats | pinned / unpinned 合并、去重，按 `updatedAt` 降序取前 6 项 |
| `priority` | Priority chats | 按 attention state 排序，再按时间降序取前 6 项 |
| `custom` | Custom assignments | 按 `AG00..AG05` 的显式绑定顺序生成 6 项 |

`priority` 的精确优先级是：

```text
waiting > unread > active > idle
```

同一优先级内使用 `recencyAt` 降序，即较新的 task 在前。

Custom assignment 持久化键为 `codex-micro-custom-agent-assignments`。每项保存：

```ts
type CustomAssignment = {
  hostId: string
  threadKey: string
  title: string | null
}
```

同一 `threadKey` 只能绑定一个 agent key；重新绑定时，应用会先清除其他 slot 上的重复绑定。应用也支持记录 `{ agentKeyId, clientThreadId }` 的 pending assignment，并在新 task 获得正式 conversation ID 后完成绑定。

### 10.3 Task 状态到 slot 状态

Local task 按以下顺序判断，靠前条件优先：

| Priority | Local state | Slot status |
|---:|---|---|
| 1 | `localStatus.status === "error"` | `error` |
| 2 | `pendingChip === "approval"` | `awaiting-approval` |
| 3 | `pendingChip === "response"` | `awaiting-response` |
| 4 | `localStatus.status === "loading"` | `working` |
| 5 | `unread === true` | `unread` |
| 6 | otherwise | `idle` |

Remote task 的规则较少：

| Priority | Remote state | Slot status |
|---:|---|---|
| 1 | latest turn failed | `error` |
| 2 | latest turn pending / in progress | `working` |
| 3 | unread | `unread` |
| 4 | otherwise | `idle` |

这里的 `waiting` / `active` 是 priority source 的 attention state；它们不是直接下发给设备的 slot status。

### 10.4 Slot 状态到 `v.oai.thstatus`

当前应用每次下发完整 6 项，并把字段压缩为 `{id,c,b,e,s,sk,sa}`：

| Slot status | RGB integer | Hex | Effect |
|---|---:|---:|---|
| `working` | `3166206` | `0x304FFE` | `solid`，选中或 pulsing 时为 `breath` |
| `unread` | `65356` | `0x00FF4C` | 同上 |
| `idle` | `16777215` | `0xFFFFFF` | 同上 |
| `awaiting-approval` | `16739584` | `0xFF6D00` | 同上 |
| `awaiting-response` | `16739584` | `0xFF6D00` | 同上 |
| `error` | `16711731` | `0xFF0033` | 同上 |
| `off` | `0` | `0x000000` | `off` |

具体转换规则：

```ts
if (slot.status === "off") {
  return { id, c: 0, b: 0, e: 0, s: 0, sk: 0, sa: 0 }
}

const breath = slot.selected || Boolean(slot.pulsing)
return {
  id,
  c: statusColor(slot.status),
  b: globalBrightness,
  e: breath ? 4 : 1,
  s: breath ? 0.4 : 0,
  sk: 0,
  sa: 0,
}
```

因此，普通非空 slot 是常亮；当前选中 slot 会持续呼吸。`pulsing` 不由普通 task slot builder 设置，供临时覆盖状态使用。空 slot 会显式下发全关配置，而不是依赖省略项保持旧值。

应用对转换后的 6 项做 JSON key 去重；payload 没变化时不会重复调用 `v.oai.thstatus`。

### 10.5 Agent key 与 task 选择

收到 `v.oai.hid` 后，主进程按下面的固定映射附加 `slot`：

```text
AG00 -> 0    AG01 -> 1    AG02 -> 2
AG03 -> 3    AG04 -> 4    AG05 -> 5
```

task 选择逻辑只处理 `act === 1`。已绑定按键会选择并导航到对应 task；同一 slot / task 在 `350 ms` 内连续触发两次时，应用额外执行 `focusWindow()`。

Custom mode 下按下未绑定的 agent key，可以进入新 task 的 pending assignment 流程；待新 task 获得正式 ID 后再固化绑定。

主进程解析按键时使用的是 `displayedLightingModel`，即最后一次实际提交到设备的 slot 模型，而不是 UI 刚计算但尚未写入的最新模型。这样在灯光写入延迟期间，物理按键仍指向用户眼前灯色所代表的 task，避免 slot 重排竞态。

### 10.6 选中 slot 与全局灯光

选中 slot 变化时，应用启动 `4 s` 的 selection highlight：

- slot 自身：只要仍为 selected，就持续使用 `breath`，不受 4 秒限制；
- keys 与 ambient zone：参与第 11 节描述的全局灯光合成。

按键或摇杆输入后有约 `100 ms` 的 input quiet window。此期间的新灯光模型只保留最新一份，计时结束后再写入，降低输入与灯光 RPC 同时占用 transport 的风险。

## 11. ChatGPT Desktop 全局灯光合成

### 11.1 输入模型与统一亮度

主进程收到的灯光模型可归纳为：

```ts
type LightingModel = {
  brightness: number
  inactivityTimeoutMs: number | null
  slots: ThreadSlot[]
  voiceState: "idle" | "recording" | "processing" | "completed"
  preserveSelectionLighting?: boolean
  snakingAmbientStatus?: ThreadSlot["status"]
  suspendDeviceStatusRefresh?: boolean
}
```

设置页亮度范围是 `0..100`、步进 `10`，进入模型时除以 100。所得 `0..1` 值同时用于：

- `v.oai.thstatus` 的所有非空 slot；
- `v.oai.rgbcfg.keys`；
- `v.oai.rgbcfg.ambient`。

应用不为不同 zone 保存独立亮度。

### 11.2 全局 zone 的合成优先级

`keys` 与 `ambient` 不是直接复制某个 slot，而是根据 overlay、voice、selected task 和 4 秒 selection highlight 合成。优先级从高到低为：

| Priority | Condition | `keys` | `ambient` |
|---:|---|---|---|
| 1 | `snakingAmbientStatus != null` | `off` | 对应 status color，`snake`，speed `0.4` |
| 2 | voice recording / processing | selection highlight 可见时跟随最终 ambient color，否则 `off` | voice color，`snake`，speed `0.4` |
| 2 | voice completed | 同上 | white，`solid` |
| 3 | selected slot 为 `working` | highlight 可见时同色 `solid`，否则 `off` | working blue，`snake`，speed `0.4` |
| 4 | 其他 selected slot，且 highlight 可见 | slot color，`solid` | slot color，`solid` |
| 5 | otherwise | `off` | `off` |

需要注意两个细节：

1. `snakingAmbientStatus` 是最高优先级，会关闭 keys，并覆盖 voice 与 selected slot；当前 UI overlay 会用到 `working`、`unread` 或 `error`。
2. voice 生效且 selection highlight 仍可见时，keys 使用的是**最终 ambient color**，因此会显示 voice color，而不是 selected slot color。

普通 selected slot 的候选配置为：

```ts
const selectedLighting = {
  effect: selected.status === "working" ? 2 : 1,
  brightness,
  speed: selected.status === "working" ? 0.4 : 0,
  magic: 0,
  color: statusColor(selected.status),
}
```

因此 working ambient 会在 4 秒结束后继续 snake；其他 selected 状态的 ambient 会在 highlight 结束后关闭。slot 自身的 breath 仍会继续。

### 11.3 Voice 与 overlay 色彩

Voice state 的固定配置：

| Voice state | Color | Effect | Speed |
|---|---:|---|---:|
| `idle` | none | 不产生覆盖 | — |
| `recording` | `0x2E8B57` | `snake` | `0.4` |
| `processing` | `0xFFFFFF` | `snake` | `0.4` |
| `completed` | `0xFFFFFF` | `solid` | `0` |

`snakingAmbientStatus` 不直接携带 RGB，而是复用 thread status color 表。例如 `error` overlay 会产生：

```json
{
  "keys": { "e": 0, "b": 0, "s": 0, "m": 0, "c": 0 },
  "ambient": { "e": 2, "b": 1, "s": 0.4, "m": 0, "c": 16711731 }
}
```

其中示例亮度为 100%。

### 11.4 Off 配置与自动熄灯

全局 off 常量是完整对象，不依赖 firmware 保留值：

```json
{
  "keys":    { "e": 0, "b": 0, "s": 0, "m": 0, "c": 0 },
  "ambient": { "e": 0, "b": 0, "s": 0, "m": 0, "c": 0 }
}
```

Auto-dim 可选值为：

```text
Off / 30 seconds / 1 minute / 3 minutes / 10 minutes / 30 minutes / 1 hour
```

到达 inactivity timeout 后，应用依次发送：

1. 全局 `v.oai.rgbcfg` off；
2. 6 个 slot 的 `v.oai.thstatus` off。

以下活动会恢复最近的完整灯光模型并重新计时：

- 任意 HID key event；
- joystick `distance > 0.1`；
- 任意 `updateLighting` 模型变化，包括 task slot 的颜色或状态变化。

服务停止时也会显式关闭全局 zone 与全部 slot。重新连接设备时则重新应用最后一个灯光模型。

### 11.5 写入顺序、去重与同步字段

一次完整更新的顺序固定为：

```text
v.oai.rgbcfg -> v.oai.thstatus
```

两次 RPC 通过同一 lighting write promise 串行化，并分别对最终 JSON 做 key 去重。只有成功写入后才更新相应 applied key；thread lighting 成功后才更新 `displayedLightingModel`。

按键或摇杆事件会创建 `100 ms` input quiet window。窗口内的新模型合并为最后一份，结束后再执行上述写入。

当前 Desktop 对每个 thread slot 始终发送：

```json
{"sk":0,"sa":0}
```

即不使用 firmware 的 `syncKeysLighting` / `syncAmbientLighting` 自动同步能力，而是由 host 通过 `v.oai.rgbcfg` 自行合成全局灯光。`magic` 当前也始终为 `0`。

## 12. 当前 ChatGPT Desktop 的实际调用子集

Codex Micro 服务实际使用：

- 发现 `Project2077 / PID 0x8360`；
- `device.status`：刷新电池，连接后立即查询，此后约每 `60 s` 查询；
- `v.oai.rgbcfg`：更新 keys 和 ambient；
- `v.oai.thstatus`：更新 6 个 thread slot；
- 不调用通用 `lights.preview`，也不消费 status 中的 profile / layer index；
- 监听 `v.oai.hid` 和 `v.oai.rad`；
- transport RPC 写操作串行化；按键/摇杆输入后约 `100 ms` 内暂缓灯光写入；
- host transport 断开后按 `1 s / 2 s / 5 s / 10 s` 退避重连。

这部分是当前 ChatGPT Desktop bundle 的行为，不应视为 firmware 协议的长期稳定保证。

## 13. 与当前 CodexMacro 实现的对照

以下为文档分析时发现的兼容性风险，仅记录，不在本次文档任务中修改代码。

### 13.1 OAI effect 类型不一致

外部 `device-kit-oai@0.1.10` 的 `v.oai.thstatus.e` 和 `v.oai.rgbcfg.*.e` 是数字 `0..6`。当前 CodexMacro：

- `ThreadLight.effect` / `LightingSide.effect` 定义为 `String`；
- 测试使用 `"breath"`；
- UI 仅在 `effect == "breath"` 时播放呼吸动画。

实际 ChatGPT Desktop 会发送 `4` 而不是 `"breath"`。当前 JSON 读取会把它保存成字符串 `"4"`，因此灯光颜色仍能显示，但 `HardwareKeys` 的呼吸判断不会命中。

### 13.2 未保存的 vendor 字段

当前 CodexMacro 会忽略：

- thread 的 `sk` / `sa`；
- ambient / keys 的 `m`。

当前 ChatGPT Desktop 通常发送 `sk: 0`、`sa: 0` 和 `m: 0`，所以暂时影响较低，但这些字段已经是协议定义的一部分。

### 13.3 Layer index 基准

`WLDeviceStatus` 声明将 `layer_index` 描述为 zero-based。CodexMacro 返回 `activeLayer`，即 `0..5`，与该定义一致。

### 13.4 已正确对齐的部分

- VID `0x303A`、PID `0x8360` 和 HID Usage Page `0xFF00`；
- Report ID `6`、RPC channel `2`、每帧 payload `61` 字节；
- 设备到主机的 HID JSON 后追加换行；
- `v.oai.hid` 的 `k` / `act` / `ag`；
- `v.oai.rad` 的 `a` / `d`；
- partial thread update 中省略字段保持当前值；
- 成功响应 `{ id, result }` 与错误响应 `{ id, error }`。

## 14. 实现兼容端时的最小要求

1. 暴露 `VID 0x303A / PID 0x8360 / Usage Page 0xFF00 / Report ID 6`。
2. 接收 channel `2`、61 字节分片的 UTF-8 JSON 请求。
3. 至少实现 `device.status`、`v.oai.thstatus` 和 `v.oai.rgbcfg`。
4. 响应必须回显请求 `id`，并使用完整键 `result` 或 `error`。
5. HID 响应和通知末尾追加 `\n`，供 host 按行提交 JSON。
6. OAI effect 使用数字 `0..6`；不要与 `lights.preview` 的字符串 effect 混用。
7. 主动输入用无 `id` 的 `v.oai.hid` / `v.oai.rad` notification。
8. 保持单个 JSON 对象小于 host/设备实际缓冲上限；外部包只对文件块明确限制 Base64 为 4096 字符。

## 15. 证据索引

主要证据来自以下 ASAR 内文件：

- `node_modules/@worklouder/device-kit-oai/package.json`
- `node_modules/@worklouder/device-kit-oai/dist/rpc_api_oai/rpc_api_oai.js`
- `node_modules/@worklouder/device-kit-oai/dist/rpc_api_oai/types/*.d.ts`
- `node_modules/@worklouder/device-kit-oai/node_modules/@worklouder/wl-device-kit/package.json`
- `node_modules/@worklouder/device-kit-oai/node_modules/@worklouder/wl-device-kit/dist/index.js`
- `node_modules/@worklouder/device-kit-oai/node_modules/@worklouder/wl-device-kit/dist/common/types/json_rpc_*.d.ts`
- `node_modules/@worklouder/device-kit-oai/node_modules/@worklouder/wl-device-kit/dist/wl_device_comm/*.d.ts`
- `node_modules/@worklouder/device-kit-oai/node_modules/@worklouder/wl-device-kit/dist/wl_rpc_api/*.d.ts`
- `node_modules/@worklouder/device-kit-oai/node_modules/@worklouder/wl-device-kit/dist/wl_rpc_api/types/*.d.ts`
- `node_modules/@worklouder/device-kit-oai/node_modules/@worklouder/wl-device-kit/dist/wl_rpc_client/*.d.ts`
- `.vite/build/codex-micro-service-DyGGZ-q3.js`，仅用于确认 ChatGPT Desktop 当前实际调用方式。
- `.vite/build/src-DU0S2Fqi.js`，用于确认 status color 常量。
- `webview/assets/codex-micro-slot-signals-BbJcKbas.js`，用于确认 slot 来源、状态与 custom assignment。
- `webview/assets/codex-micro-bridge-CE_cBpo9.js`，用于确认 agent key 选择与双击行为。
- `webview/assets/codex-micro-settings-A-ozUbrx.js`，用于确认 slot source 的 UI 语义。

升级 ChatGPT Desktop 后，应先比较包版本和 `app.asar` SHA-256，再决定本文是否仍然适用。
