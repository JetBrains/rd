#ifndef RD_CPP_ISERIALIZERSOWNER_H
#define RD_CPP_ISERIALIZERSOWNER_H

#include <unordered_set>

#include <rd_framework_export.h>

RD_PUSH_STL_EXPORTS_WARNINGS

namespace rd
{
// region predeclared

class Serializers;
// endregion

class RD_FRAMEWORK_API ISerializersOwner
{
	mutable std::unordered_set<Serializers const*> used;

public:
	// region ctor/dtor

	virtual ~ISerializersOwner() = default;
	// endregion

	void registry(Serializers const& serializers) const;

	virtual void registerSerializersCore(Serializers const& serializers) const = 0;
};
}	 // namespace rd

RD_POP_STL_EXPORTS_WARNINGS

#endif	  // RD_CPP_ISERIALIZERSOWNER_H
