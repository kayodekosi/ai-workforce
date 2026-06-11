# How to Upload to GitHub

## First: build it locally (I could not compile it here)

This was built in an environment **without Maven access to download Spring Boot**,
so I could not run the compiler. I verified structure carefully by hand (all
package declarations match paths, all braces balance, all 30 files consistent),
but you should build it once locally before pushing so your first commit is
known-good:

```bash
cd aiworkforce
mvn clean package
mvn spring-boot:run
```

Then in another terminal, confirm it responds:
```bash
curl http://localhost:8080/api/staff
```

If anything fails to compile, it'll be a small fix — paste me the error and I'll
sort it.

## Option A — GitHub website
1. github.com → **+** → **New repository**
2. Name: `ai-workforce`
3. **Public**, don't initialise with a README
4. **Create repository** → **uploading an existing file**
5. Drag in the **contents** of the `aiworkforce` folder (`src`, `docs`, `pom.xml`,
   `README.md`, `LICENSE`, `.gitignore`)
6. **Commit changes**

## Option B — git CLI
```bash
cd aiworkforce
git init
git add .
git commit -m "Initial commit: AI-Workforce configurable AI-staffed company platform"
git branch -M main
git remote add origin https://github.com/kayodekosi/ai-workforce.git
git push -u origin main
```

## After uploading

**About field:**
> Configurable AI-staffed virtual company platform — onboard AI "employees" with roles, prompts, voices and reporting lines; chat, conference, and an admin portal. Pluggable n8n/Dify/Langflow backends. Spring Boot.

**Topics:** `ai-agents`, `spring-boot`, `java`, `n8n`, `dify`, `langflow`,
`automation`, `chatbot`, `virtual-assistant`, `multi-agent`

**Highly recommended:** record a short demo GIF/video of the chat simulation and
put it at the top of the README — see `docs/GROW_THE_PROJECT.md` for how to grow it.

---

## Updating a repo you ALREADY uploaded

If you pushed an earlier version and want to bring it up to date with this one
(the compile fix, the Docker files, the mockups, the new README), you have two easy paths.

### Path A — git command line (cleanest)
From inside your local project folder that's already linked to the repo:
```bash
# copy the new files over your local copy first (or unzip this package on top), then:
git add .
git commit -m "Add Docker support, interface mockups, port docs; fix WebhookConnector compile error"
git push
```
That single push updates everything on GitHub.

If you DON'T have the repo cloned locally yet:
```bash
git clone https://github.com/kayodekosi/ai-workforce.git
cd ai-workforce
# copy the new files in (overwrite), then:
git add .
git commit -m "Update: Docker, mockups, docs, compile fix"
git push
```

### Path B — GitHub website (no git)
1. Open your repo on github.com
2. To replace a file: click it → pencil (✏️ Edit) → paste the new contents → **Commit changes**
3. To add new files (Dockerfile, docker-compose.yml, docs/images/*): **Add file → Upload files** → drag them in → **Commit changes**
4. GitHub keeps full history, so nothing is lost — each save is a new commit

### What changed in this update (so you know what to replace)
- `src/main/java/com/knatware/aiworkforce/connector/WebhookConnector.java` — compile fix
- `Dockerfile`, `docker-compose.yml`, `.dockerignore` — new
- `docs/images/` — three SVG interface mockups — new
- `README.md` — screenshots, port-change, and Docker sections added
- `docs/GITHUB_UPLOAD.md` — this section

The simplest approach: unzip this whole package over your local copy, then do the
Path A `git add . && git commit && git push`. Done.
