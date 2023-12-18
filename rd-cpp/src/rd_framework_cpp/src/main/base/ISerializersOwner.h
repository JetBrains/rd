#ifndef RD_CPP_ISERIALIZERSOWNER_H
#define RD_CPP_ISERIALIZERSOWNER_H

#include <unordered_set>

#include <rd_framework_export.h>

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

#endif	  // RD_CPP_ISERIALIZERSOWNER_H
