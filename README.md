# 人机验证 — Halo 登录注册验证插件

为 Halo 登录和注册页面添加人机验证，支持本地图形验证码、Geetest 和 Cloudflare
Turnstile。要求 Halo 2.20.0 或更高版本、Java 21。

## 功能

- 本地验证码默认启用，不依赖第三方网络服务。
- 本地验证码固定为 6 位，设置、页面输入和服务端校验使用同一长度限制。
- Geetest 和 Cloudflare Turnstile 均执行服务端二次校验。
- 验证失败时拒绝登录或注册，并限制验证码图片请求频率。
- 不在日志中记录第三方挑战令牌、签名或完整响应。

第三方数据类别、处理流程、日志、保留期限和停用方式见 [PRIVACY.md](PRIVACY.md)。

## 安装

1. 从 Releases 下载 `plugin-captcha-<version>.jar`。
2. 在 Halo 后台的“插件管理”中上传并安装。
3. 启用插件，在“人机验证设置”中选择验证服务商。

## 从源码构建

仓库采用 Halo `plugin-starter` / `create-halo-plugin` 的标准 Gradle 结构，并提交了
Gradle Wrapper。无需预装 Gradle，也不依赖开发者本机路径。

Linux / macOS：

```bash
./gradlew clean test build
```

Windows：

```powershell
.\gradlew.bat clean test build
```

构建产物位于 `build/libs/plugin-captcha-<version>.jar`。Gradle Wrapper 固定构建工具
版本，Java Toolchain 固定为 Java 21；CI 使用相同命令执行测试和构建。

## 项目结构

```text
src/main/java/       Java 插件源码
src/main/resources/  plugin.yaml、设置定义和前端脚本
src/test/java/       单元测试与登录/注册回归测试
gradle/wrapper/      可复现的 Gradle Wrapper
.github/workflows/   GitHub Actions CI
```

## 配置

| 配置项 | 说明 |
| --- | --- |
| 验证服务商 | `local`、`geetest` 或 `cloudflare` |
| 启用人机验证 | 全局开关 |
| Geetest ID / Key | Geetest 后台提供的服务端配置 |
| Cloudflare Site Key / Secret Key | Turnstile Widget 配置 |
| 图片宽度 / 高度 | 本地验证码图片尺寸，默认 130 × 48 |

## 测试

`./gradlew test` 覆盖：

- 本地验证码固定为 6 位，旧配置或非法设置不会改变长度。
- 6 位验证码通过 `/login` 与 `/registration` 认证过滤路径。
- 登录与注册页面注入的输入长度与服务端配置一致。
- Geetest 完整响应不会进入插件日志。

## 许可证

[MIT](LICENSE) © Beibing
