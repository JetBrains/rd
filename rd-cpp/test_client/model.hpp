// Copyright 1998-2018 Epic Games, Inc. All Rights Reserved.

#pragma once

#include "Identities.h"
#include "wire/SocketWire.h"
#include "Protocol.h"
#include "RdProperty.h"
#include "PumpScheduler.h"

#include <cstdint>
#include <string>

class FRiderLinkModule
{
public:
	FRiderLinkModule();
	~FRiderLinkModule();

	/** IModuleInterface implementation */
	void StartupModule();
	void ShutdownModule();

private:
	/** Handle to the test dll we will load */
	RdProperty<tl::optional<int>> test_connection{ 0 };
	RdProperty<tl::optional<std::wstring>> filename_to_open{ L"" };

	//SocketWire::Client *client = nullptr;
	std::shared_ptr<IWire> wire;
	std::unique_ptr<Protocol> clientProtocol;

	LifetimeDefinition lifetimeDef; ;
	LifetimeDefinition socketLifetimeDef;

	Lifetime lifetime;
	Lifetime socketLifetime;

	PumpScheduler clientScheduler{ "client" };
};
