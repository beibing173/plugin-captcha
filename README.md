# Plugin Captcha — Halo 人机验证插件

为 Halo 博客的登录和注册页面添加人机验证，有效防止暴力破解和恶意登录。

## ✨ 功能特性

- **多验证服务商支持** — 极验 Geetest / Cloudflare Turnstile / 本地图形验证码
- **零配置可用** — 默认内置本地图形验证码，开箱即用
- **智能验证码定位** — 自动识别注册页确认密码框位置，验证码始终插入正确位置
- **防重复提交** — 提交前校验，验证码未通过则拦截并 Toast 提示
- **验证码刷新** — 点击图片或按钮即可刷新本地验证码
- **完全离线** — 本地图形验证码不依赖任何第三方服务

## 📦 安装

1. 下载 plugin-captcha-1.0.0.jar
2. 在 Halo 后台 → 插件管理 → 上传并安装
3. 启用插件后进入「人机验证设置」选择验证服务商

## 🔧 配置

| 配置项 | 说明 |
|--------|------|
| 验证服务商 | geetest / cloudflare / local |
| 启用人机验证 | 开关 |
| 极验 ID / Key | Geetest 后台获取 |
| Cloudflare Site Key / Secret Key | Turnstile 后台获取 |
| 验证码长度 | 本地验证码字符数（默认 4） |
| 图片宽度 / 高度 | 本地验证码尺寸（默认 130×48） |

## 📸 截图

> 将截图放入 screenshots/ 目录：
> - screenshots/login-local.png — 登录页本地验证码
> - screenshots/register-local.png — 注册页本地验证码
> - screenshots/settings.png — 后台设置页

## 🛠️ 开发构建

`ash
# 编译
python compile.py

# 打包 JAR
python package.py
`

## 📄 许可证

禁止商用 · 见 [LICENSE](LICENSE) © Beibing

## 🔗 支持

- GitHub Issues: [https://github.com/halo-dev/plugin-captcha/issues](https://github.com/halo-dev/plugin-captcha/issues)
- Halo 官方论坛: [https://bbs.halo.run](https://bbs.halo.run)

---

**版本**: 1.0.0 | **要求**: Halo ≥ 2.20.0

