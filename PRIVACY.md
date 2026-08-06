# 第三方验证与数据处理说明

本插件默认使用本地图形验证码。只有站点管理员主动选择 Geetest 或 Cloudflare
Turnstile 时，登录和注册页面才会加载对应服务商脚本并调用其服务端校验接口。

## Geetest

### 数据类别

- 配置数据：`captcha_id`（前后端使用）和 `captcha_key`（仅服务端使用）。
- 单次验证数据：`lot_number`、`captcha_output`、`pass_token`、`gen_time`。
- 派生数据：服务端使用 `captcha_key` 和 `lot_number` 计算的 `sign_token`。
- 服务商脚本可能处理的浏览器、设备和网络信号，以 Geetest 的产品与隐私说明为准。

### 处理流程

1. 浏览器从 `static.geetest.com` 加载 Geetest v4 脚本。
2. 验证成功后，浏览器将四项单次验证数据随登录或注册表单提交给 Halo。
3. 插件在内存中计算 `sign_token`，通过 HTTPS 将验证数据发送到
   `gcaptcha4.geetest.com/validate`。
4. 插件只使用校验结果决定是否继续认证请求。

Geetest 官方接入流程和字段说明见
[GeeTest CAPTCHA v4 快速集成指南](https://docs.geetest.com/gt4/quick_integration_guide)。

### 日志与保留

- 插件不记录 `lot_number`、`captcha_output`、`pass_token`、`gen_time`、
  `sign_token` 或完整服务商响应。
- 插件只记录缺少必要字段、服务调用异常、响应解析异常，以及不含挑战数据的
  DEBUG 级成功/失败结果。
- 插件不把单次验证数据写入数据库或文件；请求结束后不再由插件持有。
- `captcha_id`、`captcha_key` 会作为 Halo 插件设置保留，直到管理员修改、清空或
  卸载插件。
- Geetest 自身的处理和保留期限由服务商条款及站点与服务商的合同决定。

### 停用方式

在插件设置中关闭“启用人机验证”，或切换到“本地图形验证码”并清空 Geetest
配置。彻底停用时，还应在 Geetest 控制台删除对应验证应用或轮换密钥。

## Cloudflare Turnstile

### 数据类别

- 配置数据：Site Key（前端公开）和 Secret Key（仅服务端使用）。
- 单次验证数据：`cf-turnstile-response` 令牌。
- 服务商脚本可能处理浏览器环境、行为和网络信号。Cloudflare 对其挑战信号的说明
  见 [Turnstile 概览](https://developers.cloudflare.com/turnstile/)。

### 处理流程

1. 浏览器从 `challenges.cloudflare.com` 加载 Turnstile 脚本并生成令牌。
2. 令牌随登录或注册表单提交给 Halo。
3. 插件通过 HTTPS 将令牌和 Secret Key 发送到 Cloudflare Siteverify 接口。
4. 插件只使用 `success` 结果决定是否继续认证请求。

Cloudflare 官方客户端与服务端流程见
[Turnstile Get started](https://developers.cloudflare.com/turnstile/get-started/)。

### 日志与保留

- 插件不记录 Turnstile 令牌、Secret Key 或完整服务商响应。
- 插件只记录服务调用异常、响应解析异常，以及不含令牌的 DEBUG 级成功/失败结果。
- 插件不把单次令牌写入数据库或文件；请求结束后不再由插件持有。
- Site Key 和 Secret Key 会作为 Halo 插件设置保留，直到管理员修改、清空或卸载插件。
- Cloudflare 自身的处理和保留期限由其 Turnstile 隐私条款及站点与服务商的合同决定。

### 停用方式

在插件设置中关闭“启用人机验证”，或切换到“本地图形验证码”并清空 Cloudflare
配置。彻底停用时，还应在 Cloudflare 控制台删除对应 Widget 或轮换 Secret Key。
