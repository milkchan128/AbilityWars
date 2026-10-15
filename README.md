# <img src="https://user-images.githubusercontent.com/39194432/96130737-475da400-0f33-11eb-9334-947ebd802aff.png">
<img src="https://img.shields.io/github/v/release/milkchan128/AbilityWars?style=flat-square"></img>
<img src="https://img.shields.io/github/last-commit/milkchan128/AbilityWars?style=flat-square"></img>
### 개발: [Daybreak](https://github.com/DayBreak365)
<br/>

**📋 License**

AbilityWar는 [능력자 전쟁 라이선스](https://github.com/DayBreak365/AbilityWar/blob/master/LICENSE.md)에 따라 라이선스가 부여됩니다.

**📎 Contribute**

새로운 도전, 무한한 가능성. 추가 개발은 모든 개발자에게 허용됩니다. 자신만의 Fork를 만들고 자신만의 Build를 만드세요.
모두에게 알리고 싶은 참신한 코드를 작성했다면 Pull Request를 통해 플러그인에 기여해주세요.

**🔧 Build**
- **Paper·Purpur 1.21.11 전용 빌드 (이 포크)**
    - JDK 21을 설치하고 `JAVA_HOME`을 JDK 설치 폴더로 설정하세요.
    - Windows에서는 프로젝트 루트에서 `.\gradlew.bat clean build`를 실행하세요. Linux/macOS에서는 `./gradlew clean build`를 사용하세요.
    - 배포 파일은 `build/libs/AbilityWar-1.1.0.jar`입니다. `-plain.jar`는 의존성이 포함되지 않은 파일이므로 서버에 설치하지 마세요.
    - 이 배포 파일은 **Paper와 Purpur 1.21.11만 지원**합니다. 다른 1.21.x 버전, Spigot, Folia, 다른 Paper 포크 및 26.x는 지원 대상이 아닙니다.
    - 아래 Maven 빌드와 구버전 의존성 준비는 Paper 전용 빌드에 필요하지 않습니다. 첫 Gradle 빌드는 개발용 서버 코드와 의존성을 내려받으므로 시간이 걸릴 수 있습니다.
    - 서버를 중지한 상태에서 기존 AbilityWar JAR를 백업하고 새 파일을 서버의 `plugins` 폴더에 넣으세요. AbilityWar JAR는 하나만 두세요.
    - 서버 폴더에서 `java "-Dfile.encoding=UTF-8" "-Duser.timezone=Asia/Seoul" -Xms1G -Xmx2G -jar paper-1.21.11-132.jar --nogui`로 실행할 수 있습니다. Purpur를 사용하거나 서버 파일명이 다르면 실제 파일명으로 바꾸세요.
    - 로딩 시 이 포크의 새 정식 릴리스를 확인하고 자동으로 다운로드합니다. 검증된 업데이트는 다음 서버 시작 때 적용됩니다.
    - 자동 테스트는 버전 제한을 확인합니다. `.\gradlew.bat smokeJar`로 만든 `AbilityWar-PaperSmoke.jar`는 **별도 테스트 서버에서만** 사용하세요. 주요 호환 기능과 일반 능력의 생성·정리를 검사한 뒤 서버를 자동 종료합니다.
- **Maven**
    - AbilityWar 디렉토리에서 `mvn clean package` 명령을 실행하세요. 빌드된 플러그인 파일은 `target` 폴더에서 찾을 수 있습니다.
    - AbilityWar를 로컬 Maven 레포지터리에 설치하려면 `mvn clean install` 명령을 실행하세요.
- **Craftbukkit Dependency**
    - AbilityWar 빌드시 다음 버전의 craftbukkit dependency가 필요합니다.
      - **1.12.2**, **1.13**, **1.13.2**, **1.14.4**, **1.15**, **1.16.1**, **1.16.2**, **1.16.5**, **1.17.1**, **1.19**, **1.20.4**
      - 최신 버전이 아닐 경우 **1.9.2**, **1.9.4**, **1.10.2**, **1.11.2**도 필요할 수 있음
    - [최신 버전의 BuildTools](https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar)를 내려받은 후 `java -jar BuildTools.jar --rev <version>` 명령을 이용하여 각 버전의 craftbukkit을 빌드한 후 local maven repository에 install 해주세요.
    - craftbukkit는 bukkit에 의존하므로 bukkit을 먼저 install 해야할 수 있습니다.
    - **1.19** 이상 버전의 경우, CraftBukkit/pom.xml에서 specialsource plugin을 제거한 후 clean install 해주세요.

**🔗 Download**

이 포크의 정식 배포 파일은 [GitHub Releases](https://github.com/milkchan128/AbilityWars/releases)에서 내려받을 수 있습니다. 릴리스 버전은 원본 프로젝트와 별도로 `1.0.0`부터 관리합니다.

[패치 노트](CHANGELOG.md) · [1.1.0 릴리스 노트 초안](docs/releases/v1.1.0.md) · [버전 및 릴리스 규칙](docs/RELEASING.md)

Purpur 지원은 `1.0.1`부터 적용됩니다. 기존 `1.0.0` 릴리스는 Paper만 지원하며, `1.1.0`은 현재 소스에서 빌드할 수 있습니다.

**시작 배너 및 자동 업데이트**

- 로딩 시 devhub의 우유 물방울·고리·별 로고를 옮긴 ASCII 배너와 `Powered by mlkc-studio`를 표시합니다.
- `milkchan128/AbilityWars`의 최신 정식 릴리스 태그를 현재 버전과 숫자로 비교합니다. 프리릴리스와 이전 버전은 설치하지 않습니다.
- 배포 JAR와 `SHA256SUMS.txt`를 이름으로 찾아 SHA-256, 플러그인 정보 및 1.21.11 지원 여부를 검사합니다. 네트워크나 검증 오류가 나면 현재 버전을 유지합니다.
- 기존 JAR는 `plugins/AbilityWar/backups`에 백업하고 새 JAR는 서버의 업데이트 폴더(기본 `plugins/update`)에 준비합니다. 서버를 직접 종료하거나 실행 중인 플러그인을 다시 로드하지 않습니다.
- `/aw install`도 이 포크의 릴리스를 사용하며, 선택한 새 정식 버전을 다음 서버 시작에 적용합니다.

아래 표의 구버전 링크는 원본 프로젝트의 배포 이력입니다.

| 마인크래프트 버전 | 권장 플러그인 버전 |
|:----:|:----:|
| Paper 1.21.11 | [이 포크의 최신 릴리스](https://github.com/milkchan128/AbilityWars/releases/latest) |
| Purpur 1.21.11 | `1.0.1`부터 지원 ([빌드 및 릴리스 노트 초안](docs/releases/v1.1.0.md)) |
| MC 1.20.4 | [최신 버전](https://github.com/DayBreak365/AbilityWar/releases/latest) |
| MC 1.20 ~ 1.20.3 | 지원 X |
| MC 1.19.x (Paper) | [최신 버전](https://github.com/DayBreak365/AbilityWar/releases/latest) |
| MC 1.17.x | [v3.0.0](https://github.com/DayBreak365/AbilityWar/releases/tag/20210912) |
| MC 1.12.x ~ 1.16.x | [최신 버전](https://github.com/DayBreak365/AbilityWar/releases/latest) |
| MC 1.9.x ~ 1.11.x | [v2.4.4](https://github.com/DayBreak365/AbilityWar/releases/tag/20210130) |
| MC 1.8.x | [v2.1.6.8-legacy](https://drive.google.com/uc?export=download&confirm=no_antivirus&id=1CYY7oH0IpEkH1vmL7--6fOfJ0skDgp3S) |
