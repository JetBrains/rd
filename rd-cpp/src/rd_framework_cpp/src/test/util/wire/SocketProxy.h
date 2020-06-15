#ifndef RD_CPP_TESTSOCKETPROXY_H
#define RD_CPP_TESTSOCKETPROXY_H

#include "thirdparty.hpp"

#include "base/IProtocol.h"
#include "protocol/Buffer.h"
#include "wire/SocketWire.h"

namespace rd
{
namespace util
{
class SocketProxy
{
	std::string id;
	Lifetime lifetime;
	int serverPort;
	Logger logger;

	optional<int> port;

	static constexpr int DefaultBufferSize = 16370;
	mutable Buffer serverToClientBuffer{DefaultBufferSize};
	mutable Buffer clientToServerBuffer{DefaultBufferSize};

	SequentialLifetimes serverToClientLifetime;
	SequentialLifetimes clientToServerLifetime;
	std::unique_ptr<CActiveSocket> proxyServer = std::make_unique<CActiveSocket>();
	std::unique_ptr<CPassiveSocket> proxyClient = std::make_unique<CPassiveSocket>();

	void connect(CSimpleSocket& proxyServer, CSimpleSocket& proxyClient);

	void messaging(const std::string& id, CSimpleSocket& source, CSimpleSocket& destination, Buffer& buffer,
		const SequentialLifetimes& lifetimes) const;

public:
	SocketProxy(std::string id, Lifetime lifetime, int serverPort);

	SocketProxy(const std::string& id, Lifetime lifetime, IProtocol const* protocol);

	void start();

	void StopClientToServerMessaging();

	void StartClientToServerMessaging();

	void StopServerToClientMessaging();

	void StartServerToClientMessaging();

	int getPort();
};
}	 // namespace util
}	 // namespace rd

#endif	  // RD_CPP_TESTSOCKETPROXY_H
