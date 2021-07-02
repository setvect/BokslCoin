# 1. 복슬코인

---

**경고: 본 소스코드는 누구나 사용할 수 있으며 그로 인해 발생하는 모든 문제의 책임은 사용자에게 있습니다.**

---

[업비트](https://upbit.com) Open API를 이용한 코인 자동매매 프로그램입니다.

## 1.1. 실행

### 1.1.1. 설정 파일 
- [application.yml](src/main/resources/application.yml) 참고

### 1.1.2. IDE 환경에서 실행

1. 프로그램 실행 시 업비트에서 받은 `엑세스키`, `보안키값`을 환경변수에 설정

    - Intellij 경우 `Run/Debug Configurations` ->  `Environment variables`
         ```
         ACCESS_KEY=엑세스키;SECRET_KEY=보안키
         ```
1. Active Profiles: `local`
1. `BokslCoinApplication` 클래스 실행

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
- lib/BokslCoin-0.2.0.jar: 복슬코인 프로그램
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

### 1.4.1. 변동성 돌파전략 + 손절 + 트레일링 스탑 `알고리즘 이름: vbsTrailingStop`
1. 매수 목표가 구함
    - 매수 목표가 = 전일 종가 + (전일 고가 - 전일 고가) * 변동성 돌파 비율 k
2. 현재가가 매수 목표가를 돌파 AND 이동평균 이상이면 시장가 매수
   - 이동평균 기간 설정이 `0` 이면 목표가 돌파 여부만 검사
3. 매수가 대비 수익률이 손절 가격 이하로 떨어지면 매도
4. 매수가 대비 수익룰이 트레일링 스탑 진입점에 돌파 하면 [트레일링 스탑](https://m.blog.naver.com/scruw/221976012878) 알고리즘 적용하여 시장가 매도
5. 매도 시간이 되고 현재 수익률이 `손절률`과 `트레일링 스탑`진입점 사이이면 시장가 매도
6. 매매 주기에 매매는 2번 이상 이루어 지지 않음


### 1.4.2. 이평선 돌파전략  `알고리즘 이름: mabs`
해당 알고리즘을 요약 설명하면 단기 이동평균이 장기 이동평균을 돌파(정배열) 했을 때 매수, 단기 이동 평균이 장기 이동평균 아래로 내려(역배열)가면 매도함. 

1. 단기 이동평균 값과 장기 이동편균 값을 구함
2. `(장기 이동평균 + 장기 이동평균 * 상승매수률) <= 단기 이동평균` 조건이 만족하면 매수
3. `장기 이동평균 >= (단기 이동평균 + 단기 이동평균 * 하락매도률)` 조건이 만족하면 매도
4. 매도가 발생한 주기는 매수하지 않음, 다음 주기로 넘어갔을 때 매수 활성화

※ `상승매수률`과 `하락매도률`를 둔 이유는 매수가와 매도가의 차이를 두어 매수가 이러난 직후 매도하지 않게 하기 위함 

## 1.5. 시세 데이터 클로링
- `Crawling.java`를 사용해 원하는 코인의 일봉 데이터를 수집할 수 있음
- 수집한 데이터는 백테스트에 사용

## 1.6. 백테스트
- `VbsTrailingStopBacktest.java` 변동성 돌파 전략 + 손절 + 트레일링 스탑 백테스트 소스코드
   - 소스코드상에 변수값을 조절해서 테스트 결과 얻음

## 1.7. Slack 메시지
- 아래와 같은 이벤트 발생 시 지정한 SlackBot을 통해 메시지 전송
    - 새로운 매매 주기 진입 매수 목표가
    - 매수 이벤트 발생 시
    - 매도 이벤트 발생 시
    - 어플리케이션에서 발생하는 오류

## 1.7. 참고

- [업비트 공식 문서](https://docs.upbit.com)
- [조코딩-코인자동 매매](https://github.com/youtube-jocoding/pyupbit-autotrade)
