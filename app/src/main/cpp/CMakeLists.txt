cmake_minimum_required(VERSION 3.18.1)

project("mediacodec")
#声明头文件路径
set(INCLUDE_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/include)
#声明库文件路径
set(LIB_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/lib)
#libYuv
set(YUV_LIB yuv)
set(ANDROID_LIB z android log GLESv3 OpenSLES)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}  -L${LIB_DIR}/${ANDROID_ABI} -std=gnu++11")

#设置头文件
include_directories(${INCLUDE_DIR})

#生成yuvUtil共享库
add_library(
        mediaUtil
        SHARED
        nativeMedia.cpp
        YuvUtil.cpp
)

target_link_libraries(
        mediaUtil
        #链接yuv库
        ${YUV_LIB}
        #链接本地日志库
        ${ANDROID_LIB}
)
