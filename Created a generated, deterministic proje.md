Created a generated, deterministic project reconstruction at `/Users/imsiddique/.codex/worktrees/c68a/shopizer/AGENTS.md` and a regeneration script at `/Users/imsiddique/.codex/worktrees/c68a/shopizer/tools/generate_agents_md.py`.

**Key Findings**
- Root build file: `/Users/imsiddique/.codex/worktrees/c68a/shopizer/pom.xml`
- Root packaging: `pom`
- Root GAV: `com.shopizer:shopizer:3.2.5`
- External parent: `org.springframework.boot:spring-boot-starter-parent:2.5.12`
- Declared modules: `sm-core-model`, `sm-core-modules`, `sm-core`, `sm-shop-model`, `sm-shop`

- Internal module dependency graph:
- `sm-core-model` -> none
- `sm-core-modules` -> `sm-core-model`
- `sm-core` -> `sm-core-model`, `sm-core-modules`
- `sm-shop-model` -> `sm-core-model`
- `sm-shop` -> `sm-core`, `sm-core-model`, `sm-shop-model`

- Shared/common modules detected:
- `sm-core-model` (foundation/shared model; highest in-degree)
- `sm-shop-model` (shared API model module used by `sm-shop`)

- Parent-child relationships:
- All five module POMs inherit from root `shopizer` parent POM
- Root POM inherits from Spring Boot starter parent

- Java/compiler configuration:
- `java.version=17`
- `maven.compiler.source=${java.version}`
- `maven.compiler.target=${java.version}`
- `maven-compiler-plugin` configured with `release=${java.version}`

- Annotation processors:
- `org.mapstruct:mapstruct-processor` (root compiler plugin annotationProcessorPaths)

- Plugins/profiles highlights:
- Root POM: `maven-compiler-plugin`, `spotbugs-maven-plugin`, `nexus-staging-maven-plugin`, `maven-release-plugin`, `maven-source-plugin`
- `sm-shop`: `spring-boot-maven-plugin`
- `sm-core-model`: `release` profile with Sonatype/source/javadoc/gpg plugins

- Application entry point detected:
- `com.salesmanager.shop.application.ShopApplication` in `/Users/imsiddique/.codex/worktrees/c68a/shopizer/sm-shop/src/main/java/com/salesmanager/shop/application/ShopApplication.java`

**What AGENTS.md Contains**
- Exact root tracked-file structure
- Module-by-module deep analysis (purpose, packages, categories, entry points, beans, config properties, resources, tests)
- Parsed Java type inventory (class/interface/enum/record, inheritance, annotations)
- Build/plugin/profile/compiler summaries
- Direct module POM dependency lists
- Build/run instructions and deterministic regeneration steps
- Full content for tracked text files
- Binary assets listed with exact path, size, SHA-256, and preview metadata

**Validation**
- Tracked files in repo: `1343`
- Matched file entries in AGENTS dump: `1343` (all tracked files accounted for)
- Generated file size: `/Users/imsiddique/.codex/worktrees/c68a/shopizer/AGENTS.md` is ~4.97 MB (`145707` lines)

**Important Note**
- I did not inline full raw binary payloads (e.g., `.mmdb`, `.h2.db`, images, spreadsheets) in `AGENTS.md`; they are represented with checksums/metadata for practicality. All tracked text source/config/test/build files are embedded in full.

If you want, I can also modify the generator to emit full base64 for small binaries only (while still excluding large DB/MMDB blobs).