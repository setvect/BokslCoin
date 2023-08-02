#!/bin/bash

# 로그 파일 경로
LOG_FILE="/home/setvect/BokslCoin/logs/bokslcoin.log"
# 프로그램 재시작 명령
RESTART_COMMAND="cd '/home/setvect/BokslCoin/bin' && ./BokslCoin.sh restart"

# 이전 파일 크기 저장 위치
SIZE_FILE="/home/setvect/BokslCoin/previous_log_size"

# 이전 크기를 가져옵니다. 파일이 없다면 0으로 설정합니다.
if [ -f $SIZE_FILE ]; then
    PREVIOUS_SIZE=$(cat $SIZE_FILE)
else
    PREVIOUS_SIZE=0
fi

# 현재 로그 파일의 크기를 가져옵니다.
CURRENT_SIZE=$(stat -c%s $LOG_FILE)

# 이전 및 현재 로그 파일의 크기를 출력합니다.
echo "직전 로그 사이즈: $PREVIOUS_SIZE bytes, 지금 로그 사이즈: $CURRENT_SIZE bytes"


# 이전 크기와 현재 크기가 동일하다면 프로그램을 재시작합니다.
if [ $PREVIOUS_SIZE -eq $CURRENT_SIZE ]; then
    eval $RESTART_COMMAND
fi

# 현재 크기를 이전 크기 파일에 저장합니다.
echo $CURRENT_SIZE > $SIZE_FILE
