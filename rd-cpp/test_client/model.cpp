// Copyright 1998-2018 Epic Games, Inc. All Rights Reserved.

#include "model.hpp"

#include <fstream>

FRiderLinkModule::FRiderLinkModule() :
	lifetimeDef{ Lifetime::Eternal() }
	, socketLifetimeDef{ Lifetime::Eternal() }
	, lifetime(lifetimeDef.lifetime)
	, socketLifetime(socketLifetimeDef.lifetime)
{
	uint16_t port = 0;
	int64_t test_connection_property_id = 0;
	int64_t filename_to_open_property_id = 0;

	std::ifstream inputFile("C:\\Users\\alexander.pirogov\\unreal_port.txt");

	inputFile >> port;
	wire = std::make_shared<SocketWire::Client>(lifetime, &clientScheduler, port, "TestClient");
	clientProtocol = std::make_unique<Protocol>(Identities(), &clientScheduler, wire);

	inputFile >> test_connection_property_id;
	statics(test_connection, test_connection_property_id);
	test_connection.bind(lifetime, clientProtocol.get(), "test_connection");

	inputFile >> filename_to_open_property_id;
	statics(filename_to_open, filename_to_open_property_id);
	filename_to_open.bind(lifetime, clientProtocol.get(), "filename_to_open");
}

FRiderLinkModule::~FRiderLinkModule()
{
}

void FRiderLinkModule::StartupModule()
{
	// This code will execute after your module is loaded into memory; the exact timing is specified in the .uplugin file per-module

	test_connection.set(0xdeadbeef);
	filename_to_open.set(L"beefdeadx0");	
}

void FRiderLinkModule::ShutdownModule()
{
	// This function may be called during shutdown to clean up your module.  For modules that support dynamic reloading,
	// we call this function before unloading the module.

	// Free the dll handle
}
