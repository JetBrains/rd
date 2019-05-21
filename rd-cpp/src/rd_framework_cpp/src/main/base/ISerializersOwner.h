#ifndef RD_CPP_ISERIALIZERSOWNER_H
#define RD_CPP_ISERIALIZERSOWNER_H

#include <unordered_set>

namespace rd {
	//region predeclared

	class Serializers;
	//endregion

	class ISerializersOwner {
		mutable std::unordered_set<Serializers const*> used;
	public:
		//region ctor/dtor

		virtual ~ISerializersOwner() = default;
		//endregion

		void registry(Serializers const &serializers) const;

		virtual void registerSerializersCore(Serializers const &serializers) const = 0;
	};
}


#endif //RD_CPP_ISERIALIZERSOWNER_H
