cmake_minimum_required(VERSION 3.18.1)

project("mediacodec")

#声明LibYUV路径
set(LIBYUV_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/libyuv)
set(ANDROID_LIB z android log GLESv3 OpenSLES)
set(LIBYUV_LIB yuv_static)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}  -L${LIBYUV_DIR}/lib/${ANDROID_ABI}")

#设置libYuv头文件
include_directories(${LIBYUV_DIR}/include)

#生成yuvUtil共享库
add_library(
        yuvUtil
        SHARED
        nativeYuvUtil.cpp
        RotateNV_UV.cpp
        ConvertNV.cpp
)

target_link_libraries(
        yuvUtil
        #链接libYUV库
        ${LIBYUV_LIB}
        #链接本地日志库
        ${ANDROID_LIB}
)