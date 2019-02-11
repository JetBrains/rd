//
// Created by jetbrains on 19.11.2018.
//

#ifndef RD_CPP_ISERIALIZERSOWNER_H
#define RD_CPP_ISERIALIZERSOWNER_H


#include "Serializers.h"

namespace rd {
	class ISerializersOwner {
	public:
		void registry(Serializers const &serializers);

		virtual void registerSerializersCore(Serializers const &serializers) = 0;
	};
}


#endif //RD_CPP_ISERIALIZERSOWNER_H
