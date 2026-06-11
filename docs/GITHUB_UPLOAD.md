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
