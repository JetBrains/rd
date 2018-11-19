//
// Created by jetbrains on 23.07.2018.
//

#ifndef RD_CPP_FRAMEWORK_RNAME_H
#define RD_CPP_FRAMEWORK_RNAME_H

#include <string>

class RName {
private:
    RName *parent = nullptr;
    std::string local_name, separator;
public:
    //region ctor/dtor

    RName() = default;

    RName(const RName &other) = default;

    RName(RName &&other) noexcept = default;

    RName &operator=(const RName &other) = default;

    RName &operator=(RName &&other) noexcept = default;

    RName(RName *parent, const std::string &localName, const std::string &separator);

    explicit RName(const std::string &local_name);
    //endregion

    RName sub(const std::string &localName, const std::string &separator);

    std::string toString() const;

    static RName Empty();
};

#endif //RD_CPP_FRAMEWORK_RNAME_H
