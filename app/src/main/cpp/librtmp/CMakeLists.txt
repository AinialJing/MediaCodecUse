set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO")
#全局属性,所有.c的文件
file(GLOB rtmp_source *.c)
add_library(rtmp
        STATIC
        ${rtmp_source})