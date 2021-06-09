# 1. 복슬코인

---

**본 소스코드 누구나 사용할 수 있으며 그로인해 발생하는 모든 문제의 책임은 사용자에게 있습니다.**

---

[업비트](https://upbit.com) Open API를 이용한 코인 자동매매 프로그램입니다.

## 1.1. 실행

### 1.1.1. 설정 파일 `application.yml`

```yaml
com:
  setvect:
    bokslcoin:
      autotrading:
        api:
          # 엑세스 키: 환경 변수 또는 직접 입력
          accessKey: ${ACCESS_KEY}
          # 보안 키: 환경 변수 또는 직접 입력
          secretKey: ${SECRET_KEY}
          url: https://api.upbit.com
        schedule:
          # 시황 체크 주기
          fixedDelay: 10000
        algorithm:
          # 매매 알고리즘 지정
          name: vbs
          # 변동성 돌파 알고리즘 관련 설정값
          vbs:
            # 매수, 매도 대상 코인
            coin: KRW-BTC
            # 변동성 돌파 판단 비율
            k: 0.5
            # 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
            rate: 0.5
```

### 1.1.2. IDE 환경에서 실행

1. 프로그램 실행 시 업비트에서 받은 `엑세스키`, `보안키값`을 환경변수에 설정

    - Intellij 경우 `Run/Debug Configurations` ->  `Environment variables`
         ```
         ACCESS_KEY=엑세스키;SECRET_KEY=보안키
         ```
1. Active Profiles: `local`
1. `UpbitAutoTradingApplication` 클래스 실행

## 1.2. 빌드

### 1.2.1. 빌드 실행

```bash
gradle clean
gradle makeInstallFile
```

`build/dest`에 실행 파일 만들어짐

### 1.2.2. 빌드 파일 설명

- conf/BokslCoin.yml: 설정 파일
- conf/logback-spring.xml: logback 설정
- lib/BokslCoin-1.0.0.jar: 복슬코인 프로그램
- bin/BokslCoin.sh: Linux 실행 스크립트
- bin/BokslCoin.bat: Windows에서 실행 스크립트

## 1.3. 배포

1. 서버시간 동기화
   ```sh
   $ rdate -s time.bora.net
   ```
1. `BokslCoin.yml` 설정 변경
    - accessKey, secretKey 값 등록
    - 알고리즘 상수값 변경
1. `BokslCoin.sh` 실행권한 부여
    ```shell
    $ chmod u+x BokslCoin.sh
    ```
1. `BokslCoin.sh` 실행
    ```shell
    $ ./BokslCoin.sh
    ```

## 1.4. 매매 알고리즘

### 1.4.1. 변동성 돌파전략 `알고리즘 이름: vbs`

1. 매수 목표가 구함
    - 매수 목표가 = 전일 종가 + (전일 고가 - 전일 고가) * 변동성 돌파 비율 k
2. 매수 목표가를 돌파하면 시장가 매수
3. 매수가 되었으면 당일 장이 끝나는 시점에 시장가 매도

※ 업비트 경우 UTC 기준으로 하루을 계산함. 우리나라(UTC+9)는 오늘 09:00 ~ 다음날 09:00까지가 하루임.

### 1.4.2. TODO 추가

## 1.5. 클로링
- `Crawling.java`를 사용해 원하는 코인의 일봉 데이터를 수집할 수 있음
- 수집한 데이터는 백테스트에 사용

## 1.6. 백테스트
- 과거 데이터를 사용해 만든 알고리즘을 검증
- `VbsBacktest.java` 변동성 돌파 전략 백테스트 소스코드
  - 소스코드상에 변수값을 조절해서 테스트 결과 얻음


## 1.7. 참고

- [업비트 공식 문서](https://docs.upbit.com)
- [조코딩-코인자동 매매](https://github.com/youtube-jocoding/pyupbit-autotrade)
