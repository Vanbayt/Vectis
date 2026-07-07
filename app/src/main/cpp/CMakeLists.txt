cmake_minimum_required(VERSION 3.22.1)
project("vectis-sec")

add_library(
        vectis-sec
        SHARED
        native-lib.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        vectis-sec
        ${log-lib})
