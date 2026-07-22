# 人机验证 — Halo 登录注册验证插件

为 Halo 博客的登录和注册页面添加人机验证，有效防止暴力破解和恶意登录。

## 功能特性

- **多验证服务商支持** — 本地图形验证码 / 极验 Geetest / Cloudflare Turnstile
- **零配置可用** — 默认启用本地图形验证码，开箱即用，无需任何第三方服务
- **完全离线** — 本地图形验证码不依赖任何外部服务或网络请求
- **智能验证码定位** — 自动识别注册页确认密码框位置，验证码始终插入正确位置
- **防重复提交** — 提交前校验，验证码未通过则拦截并 Toast 提示
- **验证码刷新** — 点击图片或按钮即可刷新本地验证码
- **安全优先** — 验证码验证失败时拒绝登录（fail-closed），内置请求频率限制

## 安装

1. 下载 plugin-captcha-x.x.x.jar
2. 在 Halo 后台 -> 插件管理 -> 上传并安装
3. 启用插件后进入人机验证设置，默认已启用本地图形验证码

## 配置

| 配置项 | 说明 |
|--------|------|
| 验证服务商 | local / geetest / cloudflare |
| 启用人机验证 | 开关 |
| 极验 ID / Key | Geetest 后台获取 |
| Cloudflare Site Key / Secret Key | Turnstile 后台获取 |
| 验证码长度 | 本地验证码字符数（默认 4） |
| 图片宽度 / 高度 | 本地验证码尺寸（默认 130x48） |

## 第三方服务说明

- **本地图形验证码（默认）**：完全本地生成，无任何外部网络请求
- **极验 Geetest**：选择后前端加载 `static.geetest.com` 脚本，后端调用极验 API 验证
- **Cloudflare Turnstile**：选择后前端加载 `challenges.cloudflare.com` 脚本

## 许可证

[MIT](LICENSE) (c) Beibing

## 支持

- GitHub Issues: [https://github.com/beibing173/plugin-captcha/issues](https://github.com/beibing173/plugin-captcha/issues)

---

**要求**: Halo >= 2.20.0
