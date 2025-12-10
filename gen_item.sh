#!/bin/bash
mvn clean package -q -DskipTests && timeout 10 java -cp target/TeaMobi-1.0-RELEASE-jar-with-dependencies.jar nro.ahwuocdz.constant.ItemConstantLoader 2>&1 | grep -E "(Bắt đầu|thành công|Hoàn thành|Lỗi)"

