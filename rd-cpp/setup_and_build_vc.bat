git clone https://github.com/TartanLlama/optional.git
git clone https://github.com/mpark/variant.git
git clone https://github.com/google/googletest.git
mkdir build
cd build
cmake -G "Visual Studio 15 2017" ..
cmake --build . --config Release