//
// Created by jetbrains on 19.11.2018.
//

#ifndef RD_CPP_ISERIALIZERSOWNER_H
#define RD_CPP_ISERIALIZERSOWNER_H

namespace rd {
	//region predeclared

	class Serializers;
	//endregion

	class ISerializersOwner {
	public:
		virtual ~ISerializersOwner() = default;

		void registry(Serializers const &serializers) const;

		virtual void registerSerializersCore(Serializers const &serializers) const = 0;
	};
}


#endif //RD_CPP_ISERIALIZERSOWNER_H
