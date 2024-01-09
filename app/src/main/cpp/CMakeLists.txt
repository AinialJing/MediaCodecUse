cmake_minimum_required(VERSION 3.18.1)

project("mediacodec")

#声明LibYUV路径
set(INCLUDE_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/include)
set(LIB_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/lib)
set(FFMPEG_LIB avformat avcodec avdevice avfilter avutil swresample swscale)
set(YUV_LIB yuv)
set(ANDROID_LIB z android log GLESv3 OpenSLES)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}  -L${LIB_DIR}/${ANDROID_ABI}")

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
        #链接ffmpeg库
        ${FFMPEG_LIB}
        #链接yuv库
        ${YUV_LIB}
        #链接本地日志库
        ${ANDROID_LIB}
)
