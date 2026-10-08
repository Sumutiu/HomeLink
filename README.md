# 🏠 HomeLink – Teleport with Style!

**HomeLink** is a lightweight and fully server-side **Fabric** mod that lets players create, manage, and teleport to custom home locations with ease.  
Perfect for survival servers, SMPs, and adventure-based communities.

---

## ✨ Features

- 📌 Set and delete named home locations  
- 🧭 Teleport to your homes using `/home <name>`  
- ↩️ Return to where you were before your last teleport with `/tpback`  
- 👥 Send and accept teleport requests to and from other players  
- ⛔ Pending teleports are cancelled by damage, and by movement *(configurable)*  
- 🛡 Short damage protection after arriving *(configurable)*  
- 📁 Saves homes per-player in easy-to-read JSON files  
- 🌍 Multi-world and nether-friendly  
- 💬 Intuitive feedback messages and logging  
- 🛡 **Fully server-side** — no client mod required!

---

## 🔧 Commands

- `/sethome <name>` – Set a home at your current location  
- `/delhome <name>` – Delete a specific home  
- `/home [name]` – Teleport to a home (without a name: your home called "home", or your only home)  
- `/tpback` – Return to where you were before your last teleport  
- `/tpto <player>` – Request to teleport to another player  
- `/tphere <player>` – Request another player to teleport to you  
- `/tpaccept <player>` – Accept a pending request  
- `/tpdeny <player>` – Deny a pending request  
- `/tpcancel` – Cancel your pending teleport

---

## ⚙️ Configuration

Edit the config file located at:  
`/config/HomeLink/HomeLink.json`

You can configure:
- Max number of homes
- Home delay (`/home`)
- Back delay (`/tpback`)
- Teleport delay (`/tpto`, `/tphere`)
- Whether teleport cancels on movement
- Teleport request accept timeout
- Damage protection time after arriving

---

## 🧩 Requirements

- [Fabric Loader](https://fabricmc.net/use/)
- [Fabric API](https://modrinth.com/mod/fabric-api)

---

## 🌟 Why HomeLink?

Simple to use, server-friendly, and packed with quality-of-life features.  
Whether you're a casual player or running a large server, **HomeLink** makes teleportation smooth, fair, and configurable.

---

## 📜 License

This mod is licensed under the GNU AGPLv3 Licence.  

---

## 💬 Feedback

Found a bug or have a feature suggestion?  
Open an issue or PR on GitHub!
