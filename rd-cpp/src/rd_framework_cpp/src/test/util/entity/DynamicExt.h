#ifndef RD_CPP_DYNAMICEXT_H
#define RD_CPP_DYNAMICEXT_H


#include "RdExtBase.h"
#include "RdProperty.h"

namespace rd {
	namespace test {
		namespace util {
			class DynamicExt : public RdExtBase, public IPolymorphicSerializable {
			public:
				RdProperty<std::wstring> bar{L""};
				std::wstring debugName;

				DynamicExt();

				DynamicExt(RdProperty<std::wstring> bar, std::wstring debugName);

				DynamicExt(std::wstring const &bar, std::wstring const &debugName);


				void init(Lifetime lifetime) const override;

				void identify(const Identities &identities, RdId const &id) const override;

				static DynamicExt read(SerializationCtx  &ctx, Buffer &buffer);

				void write(SerializationCtx  &ctx, Buffer &buffer) const override;

				static void create(IProtocol *protocol);

				size_t hashCode() const noexcept override;

				bool equals(const ISerializable &serializable) const override;

				std::string type_name() const override;

				static std::string static_type_name();
			};
		}
	}
}


#endif //RD_CPP_DYNAMICEXT_H
