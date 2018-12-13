# MPark.Variant
#
# Copyright Michael Park, 2015-2017
#
# Distributed under the Boost Software License, Version 1.0.
# (See accompanying file LICENSE.md or copy at http://boost.org/LICENSE_1_0.txt)

cmake_minimum_required(VERSION 3.6.3)

project(MPark.Variant VERSION 1.3.0 LANGUAGES CXX)

# Target.
add_library(mpark_variant INTERFACE)
target_include_directories(mpark_variant INTERFACE
  $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>)

# Install.
install(DIRECTORY include/mpark DESTINATION ${CMAKE_INSTALL_PUBLIC_HEADER})
