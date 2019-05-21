cmake_minimum_required(VERSION 2.8 FATAL_ERROR)

if (POLICY CMP0048)
    # cmake warns if loaded from a min-3.0-required parent dir, so silence the warning:
    cmake_policy(SET CMP0048 NEW)
endif()

project(clsocket)

# set up versioning.
set(BUILD_MAJOR "1")
set(BUILD_MINOR "4")
set(BUILD_VERSION "3")
set(BUILD_VERSION ${BUILD_MAJOR}.${BUILD_MINOR}.${BUILD_VERSION})

SET(CLSOCKET_HEADERS
    src/ActiveSocket.h
    src/Host.h
    src/PassiveSocket.h
    src/SimpleSocket.h
    src/StatTimer.h
)

SET(CLSOCKET_SOURCES
    src/SimpleSocket.cpp
    src/ActiveSocket.cpp
    src/PassiveSocket.cpp
)

# mark headers as headers...
SET_SOURCE_FILES_PROPERTIES( ${CLSOCKET_HEADERS} PROPERTIES HEADER_FILE_ONLY TRUE )
# append to sources so that dependency checks work on headers
LIST(APPEND CLSOCKET_SOURCES ${CLSOCKET_HEADERS})

OPTION(CLSOCKET_SHARED "Build clsocket lib as shared." OFF)
OPTION(CLSOCKET_DEP_ONLY "Build for use inside other CMake projects as dependency." ON)

# make the lib
if(CLSOCKET_SHARED)
    if(CLSOCKET_DEP_ONLY)
        ADD_LIBRARY(clsocket SHARED EXCLUDE_FROM_ALL ${CLSOCKET_SOURCES})
    else()
        ADD_LIBRARY(clsocket SHARED ${CLSOCKET_SOURCES})
    endif()
else()
    if(CLSOCKET_DEP_ONLY)
        ADD_LIBRARY(clsocket STATIC EXCLUDE_FROM_ALL ${CLSOCKET_SOURCES})
    else()
        ADD_LIBRARY(clsocket STATIC ${CLSOCKET_SOURCES})
    endif()
endif()

# install into configured prefix
if(NOT CLSOCKET_DEP_ONLY)
    install(TARGETS clsocket ARCHIVE DESTINATION lib LIBRARY DESTINATION lib)
    install(FILES ${CLSOCKET_HEADERS} DESTINATION include)
else()

endif()

set_target_properties(clsocket PROPERTIES VERSION ${BUILD_VERSION}
                                          SOVERSION ${BUILD_MAJOR})

target_include_directories(clsocket PUBLIC src)
                                          

# OS and compiler checks. 
if(UNIX)
    if(APPLE)
        # Special Apple stuff here
        target_compile_definitions(clsocket PUBLIC _DARWIN)
    else()
        target_compile_definitions(clsocket PUBLIC _LINUX)
        if(CYGWIN)
            # Special Cygwin stuff here
        endif()
    endif()
elseif(WIN32)
    target_compile_definitions(clsocket PUBLIC _WIN32)
    SET(PROJECT_LIBS Ws2_32.lib)
    if(MINGW)
        # Special MINGW stuff here
    elseif(MSVC)
        # Special MSVC stuff here
        target_compile_definitions(clsocket PUBLIC _WINSOCK_DEPRECATED_NO_WARNINGS)
    else()
        # No idea what it is, but there's a good chance it's too weird.
        MESSAGE( FATAL_ERROR "Using unknown WIN32 compiler... NOT. Please add to build system." )
    endif()
endif()

TARGET_LINK_LIBRARIES(clsocket ${PROJECT_LIBS})

if(UNIX)
    OPTION(CLSOCKET_EXAMPLES "Build the examples" OFF)

    if(CLSOCKET_EXAMPLES)
        ADD_EXECUTABLE(clsocket-example examples/RecvAsync.cpp)
        TARGET_LINK_LIBRARIES(clsocket-example clsocket pthread)
        if(NOT CLSOCKET_DEP_ONLY)
            install(TARGETS clsocket-example DESTINATION bin)
        endif()

        ADD_EXECUTABLE(querydaytime-example examples/QueryDayTime.cpp)
        TARGET_LINK_LIBRARIES(querydaytime-example clsocket)

        ADD_EXECUTABLE(echoserver-example examples/EchoServer.cpp)
        TARGET_LINK_LIBRARIES(echoserver-example clsocket)
    endif()
endif()

install(DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/
        DESTINATION "${CMAKE_INSTALL_PUBLIC_HEADER}/clsocket"
        CONFIGURATIONS Release
        FILES_MATCHING
                PATTERN *.h
                PATTERN *.hpp
)