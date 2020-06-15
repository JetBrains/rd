#include "SocketProxy.h"

#include <utility>

namespace rd
{
namespace util
{
void SocketProxy::connect(CSimpleSocket& proxyServer, CSimpleSocket& proxyClient)
{
	try
	{
		logger.info("Connecting proxies between themselves...");

		auto task1 = std::async([&] {
			messaging("Server to client", proxyServer, proxyClient, serverToClientBuffer, serverToClientLifetime);
		}).share();

		auto task2 = std::async([&] {
			messaging("Client to server", proxyClient, proxyServer, clientToServerBuffer, clientToServerLifetime);
		}).share();

		lifetime->add_action([task1 = std::move(task1), task2 = std::move(task2)]() {
			task1.wait();
			task2.wait();
		});
		logger.info(id + ": transferring messages started");
	}
	catch (std::exception const& e)
	{
		logger.error(&e, id + ": connecting proxies failed");
	}
}

void SocketProxy::messaging(const std::string& id, CSimpleSocket& source, CSimpleSocket& destination, Buffer& buffer,
	const SequentialLifetimes& lifetimes) const
{
	while (!lifetime->is_terminated())
	{
		try
		{
			int32_t length = source.Receive(DefaultBufferSize, buffer.data());
			if (length == -1)
			{
				throw std::runtime_error(id +
										 ": error occurred while receive"
										 ", reason: " +
										 source.DescribeError());
			}
			if (length == 0)
			{
				throw std::runtime_error(id +
										 ": connection lost on receive"
										 ", reason: " +
										 source.DescribeError());
			}

			logger.info(id + ": message of length: %d was read", length);
			if (!lifetimes.is_terminated())
			{
				int32_t total_sent = 0;
				while (total_sent < length)
				{
					int32_t sent = destination.Send(buffer.data() + total_sent, length - total_sent);
					if (sent == -1)
					{
						throw std::runtime_error(id +
												 ": error occurred while send"
												 ", reason: " +
												 destination.DescribeError());
					}
					if (sent == 0)
					{
						throw std::runtime_error(id +
												 ": connection lost on send"
												 ", reason: " +
												 destination.DescribeError());
					}
					total_sent += sent;
					logger.info(id + ": piece of message of length: %d was written", sent);
				}
				logger.info(id + ": message of length: %d was fully written", length);
			}
			else
			{
				logger.info(id + ": message of length %d was not transferred, because lifetime was terminated", length);
			}
		}
		catch (std::exception const& e)
		{
			logger.error(&e, id + ": messaging failed");
			break;
		}
	}
}

SocketProxy::SocketProxy(std::string id, Lifetime lifetime, int serverPort)
	: id(std::move(id))
	, lifetime(lifetime)
	, serverPort(serverPort)
	, serverToClientLifetime(lifetime)
	, clientToServerLifetime(lifetime)
{
	serverToClientLifetime.next();
	clientToServerLifetime.next();

	lifetime->add_action([this]() {
		port = {};

		StopServerToClientMessaging();
		StopClientToServerMessaging();

		proxyServer->Close();
		proxyClient->Close();
	});
}

SocketProxy::SocketProxy(const std::string& id, Lifetime lifetime, IProtocol const* protocol)
	: SocketProxy(std::move(id), std::move(lifetime), dynamic_cast<SocketWire::Server const*>(protocol->get_wire())->port)
{
}

void SocketProxy::start()
{
	try
	{
		logger.info("Creating proxies for server and client...");
		proxyServer = std::make_unique<CActiveSocket>();
		if (!proxyServer->Initialize())
		{
			throw std::runtime_error(id +
									 ": initialize failed"
									 ", reason: " +
									 proxyServer->DescribeError());
		}
		proxyServer->DisableNagleAlgoritm();
		if (!proxyServer->Open("127.0.0.1", serverPort))
		{
			throw std::runtime_error(id +
									 ": open failed"
									 ", reason: " +
									 proxyServer->DescribeError());
		}

		proxyClient = std::make_unique<CPassiveSocket>();
		if (!proxyClient->Initialize())
		{
			throw std::runtime_error(id +
									 ": initialize failed"
									 ", reason: " +
									 proxyClient->DescribeError());
		}
		if (!proxyClient->Listen("127.0.0.1", 0))
		{
			throw std::runtime_error(id +
									 ": listen failed"
									 ", reason: " +
									 proxyClient->DescribeError());
		}

		port = proxyClient->GetServerPort();
		logger.info("Proxies for server on port %d and client on port %d created successfully", serverPort, port);

		auto thread = std::make_shared<std::thread>([this] {
			CActiveSocket* accepted_client = proxyClient->Accept();
			if (accepted_client == nullptr)
			{
				throw std::runtime_error(id +
										 ": accept returns nullptr"
										 ", reason:  " +
										 proxyClient->DescribeError());
			}

			accepted_client->DisableNagleAlgoritm();

			logger.info("New client connected on port %d", port);

			connect(*proxyServer, *accepted_client);
		});

		lifetime->add_action([thread] { thread->join(); });
	}
	catch (std::exception const& e)
	{
		logger.error(&e, "Failed to create proxies");
	}
}

void SocketProxy::StopClientToServerMessaging()
{
	clientToServerLifetime.terminate_current();
}

void SocketProxy::StartClientToServerMessaging()
{
	clientToServerLifetime.next();
}

void SocketProxy::StopServerToClientMessaging()
{
	serverToClientLifetime.terminate_current();
}

void SocketProxy::StartServerToClientMessaging()
{
	serverToClientLifetime.next();
}

int SocketProxy::getPort()
{
	if (!port)
	{
		throw std::runtime_error("SocketProxy was not started");
	}
	return port.value();
}
}	 // namespace util
}	 // namespace rd
