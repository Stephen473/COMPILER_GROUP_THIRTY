# Setup Guide: Java, Maven, Git and GitHub

Follow this once on your own laptop. It takes about 30 minutes. At the end you
will have built the compiler and pushed a first commit.

Commands marked **Windows** go in PowerShell or Command Prompt. **macOS/Linux**
commands go in Terminal. Close and reopen your terminal after installing
anything, so it picks up the new settings.

---

## Step 1: Install the JDK (Java 17 or newer)

1. Go to **https://adoptium.net** and download **Temurin 17 (LTS)** for your system.
2. **Windows:** run the installer. On the "Custom Setup" page, enable
   **"Set JAVA_HOME variable"** and **"Add to PATH"** (click each one and choose
   "Will be installed on local hard drive").
3. **macOS:** open the `.pkg` and click through.
   **Ubuntu/Debian:** `sudo apt update && sudo apt install openjdk-17-jdk`
4. Open a **new** terminal and check:

```bash
java -version
javac -version
```

Both must show version 17 or higher. If `javac` is "not recognized", you
installed a JRE instead of a JDK, or PATH was not set; see Troubleshooting.

Check `JAVA_HOME` too (Maven needs it):

```bash
echo %JAVA_HOME%          # Windows Command Prompt
echo $env:JAVA_HOME       # Windows PowerShell
echo $JAVA_HOME           # macOS/Linux
```

If it is empty on Windows: Start menu, type "environment variables", open
"Edit the system environment variables", Environment Variables, New (under User
variables), name `JAVA_HOME`, value = the JDK folder, for example
`C:\Program Files\Eclipse Adoptium\jdk-17.0.x-hotspot`.

---

## Step 2: Install Maven

1. Go to **https://maven.apache.org/download.cgi** and download the latest
   **Binary zip** (Windows) or **Binary tar.gz** (macOS/Linux), version 3.9.x.
2. Extract it somewhere simple:
   - Windows: `C:\Tools\apache-maven-3.9.x`
   - macOS/Linux: `~/tools/apache-maven-3.9.x`
3. Add its `bin` folder to PATH.
   - **Windows:** Environment Variables, select `Path` under User variables,
     Edit, New, paste `C:\Tools\apache-maven-3.9.x\bin`, OK.
   - **macOS/Linux:** add this line to `~/.zshrc` (or `~/.bashrc`), then run
     `source ~/.zshrc`:
     ```bash
     export PATH="$HOME/tools/apache-maven-3.9.x/bin:$PATH"
     ```
   - **macOS with Homebrew:** `brew install maven`. **Ubuntu:** `sudo apt install maven`.
4. Open a **new** terminal and check:

```bash
mvn -v
```

It should print the Maven version and "Java version: 17" or higher.

---

## Step 3: Install Git

1. **Windows:** download from **https://git-scm.com/download/win** and run the
   installer. The default options are fine. Make sure **Git Credential Manager**
   stays ticked (it handles GitHub sign-in).
2. **macOS:** run `git --version`; if it is missing, macOS offers to install it.
   Or `brew install git`. **Ubuntu:** `sudo apt install git`.
3. Check:

```bash
git --version
```

4. Tell Git who you are. **Use the same email as your GitHub account**, otherwise
   your commits will not be linked to your profile and will not show as yours:

```bash
git config --global user.name  "Your Full Name"
git config --global user.email "your-github-email@example.com"
git config --global init.defaultBranch main
git config --global pull.rebase false
```

Check with `git config --global --list`.

---

## Step 4: GitHub account and the invitation

1. If you have no account, sign up at **https://github.com**. Verify your email.
2. The group leader adds you to `COMPILER_GROUP_30` as a collaborator. GitHub
   sends an **invitation email**. Open it and click **Accept invitation**
   (or go to https://github.com/notifications). Until you accept, you cannot push.
3. Open the repository page in your browser and confirm you can see it. It is
   private, so it will show a 404 if you are not signed in or not yet invited.

---

## Step 5: Sign in to GitHub from your laptop

The repository is private, and GitHub no longer accepts your account password
on the command line. Use **one** of these. Option A is the easiest.

### Option A: Git Credential Manager (Windows) or GitHub CLI (any system)

**Windows, with Git installed from Step 3:** do nothing now. The first time you
`git clone` or `git push`, a browser window opens. Choose **Sign in with your
browser**, approve, and it remembers you.

**GitHub CLI (works on every system):**

1. Install from **https://cli.github.com** (Windows: `winget install GitHub.cli`,
   macOS: `brew install gh`, Ubuntu: see the site).
2. Run:

```bash
gh auth login
```

3. Choose GitHub.com, HTTPS, "Login with a web browser", and follow the prompts.
4. Check with `gh auth status`.

### Option B: Personal Access Token

Use this if the browser sign-in fails.

1. On GitHub: your profile picture, Settings, Developer settings, Personal access
   tokens, **Tokens (classic)**, Generate new token (classic).
2. Name it `compiler-laptop`, set an expiry (90 days is fine), tick the **repo** scope.
3. Click Generate and **copy the token now**; GitHub shows it only once.
4. When Git asks for a **password**, paste the token. The username is your GitHub username.

Never put the token in a file, in the repository, or in a chat.

---

## Step 6: Clone the repository

Pick a folder for your projects, for example `Documents\projects`, then:

```bash
cd Documents/projects
git clone https://github.com/<OWNER>/COMPILER_GROUP_30.git
cd COMPILER_GROUP_30
```

Replace `<OWNER>` with the account that owns the repository (the group leader will
tell you the exact URL). Your sign-in prompt from Step 5 appears here if needed.

Look around: `ls` (macOS/Linux) or `dir` (Windows) should show `pom.xml`, `src`,
`docs`, `examples` and `README.md`. If you see only a folder inside a folder, you
are one level too high or too deep; `pom.xml` must be in the folder you are in.

---

## Step 7: Build, test and run

From the repository root (where `pom.xml` is):

```bash
mvn clean package
java -jar target/compiler-group-30-1.0-SNAPSHOT.jar examples/sample.ey
```

The first build downloads libraries and can take a few minutes. You should see
`BUILD SUCCESS`, then a list of tokens. Try `examples/errors.ey` too; it
should print five lexical errors.

---

## Step 8: Pick an editor

Any of these works:

- **IntelliJ IDEA Community** (https://www.jetbrains.com/idea/download): File,
  Open, choose the folder with `pom.xml`, and "Trust Project". It reads the Maven setup automatically.
- **VS Code** (https://code.visualstudio.com): install the **Extension Pack for
  Java**, then File, Open Folder on the repository.

If the editor asks which JDK to use, choose the Temurin 17 you installed.

---

## Step 9: Your first commit (proves everything works)

This checks that you can write to the repository. Each person adds **their own
file**, so nobody edits the same lines and there are no merge conflicts.
The group leader must have pushed the foundation first.

```bash
git checkout main
git pull
git checkout -b setup/your-name
mkdir -p docs/team
```

Create the file `docs/team/firstname-lastname.md` (all lowercase, for example
`docs/team/ama-mensah.md`) with this content, using your own details:

```
# Your Full Name

- Index number: 12345678
- GitHub username: your-username
- Phase 1 stage: (the stage number you were given)
```

Then:

```bash
git add docs/team/firstname-lastname.md
git commit -m "Docs: add <your name> to the team"
git push -u origin setup/your-name
```

On GitHub you will see a "Compare & pull request" button. Click it, create the
pull request, and ask a teammate to review and merge it. After it is merged:

```bash
git checkout main
git pull
```

Now read [GIT_WORKFLOW.md](GIT_WORKFLOW.md); it is how we work from here on.

---

## Daily routine (short version)

```bash
git checkout main && git pull           # start of every session
git checkout -b feature/short-name      # one branch per feature
# ...write code...
mvn test                                # must pass before you push
git add -A
git commit -m "Scanner: what you changed"
git push -u origin feature/short-name   # then open a pull request
```

Commit small and often. The course requires a visible, incremental history.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `'java' is not recognized` | Java is not on PATH. Reinstall Temurin with "Add to PATH" ticked, or add `...\jdk-17...\bin` to PATH. Open a **new** terminal. |
| `javac` not found but `java` works | You have a JRE, not a JDK. Install Temurin **JDK**. |
| `mvn` says `JAVA_HOME is not defined correctly` | Set `JAVA_HOME` to the JDK folder (Step 1), not the `bin` folder. Open a new terminal. |
| `mvn` says `release version 17 not supported` | Maven is using an older Java. `mvn -v` shows which. Fix `JAVA_HOME`. |
| `mvn` is not recognized (PowerShell) or `mvn: command not found` | Maven is not installed, or its `bin` folder is not on PATH (Step 2). Install it, then open a **new** terminal. Run `where.exe mvn` (Windows) or `which mvn` to check. |
| `Could not transfer artifact ... repo.maven.apache.org` | No internet, or a campus proxy/firewall is blocking Maven Central. Try a phone hotspot. |
| `git clone` says `Repository not found` | You have not accepted the invitation, or you are signed in to the wrong GitHub account. Check https://github.com/notifications and `gh auth status`. |
| `Authentication failed` / password rejected | GitHub rejects account passwords. Use the browser sign-in or a Personal Access Token (Step 5). |
| `Permission denied` / 403 on `git push` | Your invitation is not accepted, or you were added with read access only. Ask the group leader to check your role (Settings, Collaborators). |
| Commits show up as an unknown user on GitHub | `user.email` does not match your GitHub email. Fix it with `git config --global user.email ...` (new commits only). |
| `git pull` reports a merge conflict | See "Merge conflicts" in [GIT_WORKFLOW.md](GIT_WORKFLOW.md). |
| `Please tell me who you are` | You skipped the `git config` commands in Step 3. |

---

## Final checklist

Tick each one before you start coding:

- [ ] `java -version` and `javac -version` show 17 or higher
- [ ] `mvn -v` works and shows Java 17 or higher
- [ ] `git --version` works and `git config --global --list` shows your name and GitHub email
- [ ] I accepted the GitHub invitation and can see the repository
- [ ] I cloned the repository and `mvn clean package` ends with `BUILD SUCCESS`
- [ ] The compiler runs on `examples/sample.ey`
- [ ] I added my file in `docs/team/`, opened a pull request and had it merged
