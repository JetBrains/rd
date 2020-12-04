#include "SocketProxy.h"

#include "spdlog/sinks/stdout_color_sinks.h"

#include <SimpleSocket.h>
#include <ActiveSocket.h>
#include <PassiveSocket.h>

#include <utility>

namespace rd
{
namespace util
{
void SocketProxy::connect(CSimpleSocket& proxyServer, CSimpleSocket& proxyClient)
{
	try
	{
		logger->info("Connecting proxies between themselves...");

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
		logger->info("{}: transferring messages started", id);
	}
	catch (std::exception const& e)
	{
		logger->error("{}: connecting proxies failed {}", id, e.what());
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

			logger->info("{}: message of length: {} was read", id, length);
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
					logger->info("{}: piece of message of length: {} was written", id, sent);
				}
				logger->info("{}: message of length: {} was fully written", id, length);
			}
			else
			{
				logger->info("{}: message of length {} was not transferred, because lifetime was terminated", id, length);
			}
		}
		catch (std::exception const& e)
		{
			logger->error("{}: messaging failed | {}", id, e.what());
			break;
		}
	}
}

SocketProxy::SocketProxy(std::string id, Lifetime lifetime, int serverPort)
	: id(std::move(id))
	, lifetime(lifetime)
	, serverPort(serverPort)
	, logger(spdlog::stderr_color_mt<spdlog::synchronous_factory>("socketProxyLog", spdlog::color_mode::automatic))
	, serverToClientLifetime(lifetime)
	, clientToServerLifetime(lifetime)
	, proxyServer(std::make_unique<CActiveSocket>())
	, proxyClient(std::make_unique<CPassiveSocket>())
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
		logger->info("Creating proxies for server and client...");
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
		logger->info("Proxies for server on port {} and client on port {} created successfully", serverPort, port.value());

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

			logger->info("New client connected on port {}", port.value());

			connect(*proxyServer, *accepted_client);
		});

		lifetime->add_action([thread] { thread->join(); });
	}
	catch (std::exception const& e)
	{
		logger->error("Failed to create proxies | {}", e.what());
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

SocketProxy::~SocketProxy() = default;

}	 // namespace util
}	 // namespace rd
