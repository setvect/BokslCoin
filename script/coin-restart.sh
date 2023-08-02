#!/bin/bash

# �α� ���� ���
LOG_FILE="/home/setvect/BokslCoin/logs/bokslcoin.log"
# ���α׷� ����� ���
RESTART_COMMAND="cd '/home/setvect/BokslCoin/bin' && ./BokslCoin.sh restart"

# ���� ���� ũ�� ���� ��ġ
SIZE_FILE="/home/setvect/BokslCoin/previous_log_size"

# ���� ũ�⸦ �����ɴϴ�. ������ ���ٸ� 0���� �����մϴ�.
if [ -f $SIZE_FILE ]; then
    PREVIOUS_SIZE=$(cat $SIZE_FILE)
else
    PREVIOUS_SIZE=0
fi

# ���� �α� ������ ũ�⸦ �����ɴϴ�.
CURRENT_SIZE=$(stat -c%s $LOG_FILE)

# ���� �� ���� �α� ������ ũ�⸦ ����մϴ�.
echo "���� �α� ������: $PREVIOUS_SIZE bytes, ���� �α� ������: $CURRENT_SIZE bytes"


# ���� ũ��� ���� ũ�Ⱑ �����ϴٸ� ���α׷��� ������մϴ�.
if [ $PREVIOUS_SIZE -eq $CURRENT_SIZE ]; then
    eval $RESTART_COMMAND
fi

# ���� ũ�⸦ ���� ũ�� ���Ͽ� �����մϴ�.
echo $CURRENT_SIZE > $SIZE_FILE
