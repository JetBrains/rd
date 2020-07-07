#ifndef RD_CPP_FRAMEWORK_RNAME_H
#define RD_CPP_FRAMEWORK_RNAME_H

#include "thirdparty.hpp"

#include <string>

namespace rd
{
class RNameImpl;

/**
 * \brief Recursive name. For constructs like Aaaa.Bbb::CCC
 */
class RName
{
public:
	// region ctor/dtor

	RName() = default;

	RName(const RName& other) = default;

	RName(RName&& other) noexcept = default;

	RName& operator=(const RName& other) = default;

	RName& operator=(RName&& other) noexcept = default;

	RName(RName parent, string_view localName, string_view separator);

	explicit RName(string_view local_name);
	// endregion

	RName sub(string_view localName, string_view separator);

	explicit operator bool() const
	{
		return impl != nullptr;
	}

	friend std::string to_string(RName const& value);

private:
	std::shared_ptr<RNameImpl> impl;
};
}	 // namespace rd

#endif	  // RD_CPP_FRAMEWORK_RNAME_H
