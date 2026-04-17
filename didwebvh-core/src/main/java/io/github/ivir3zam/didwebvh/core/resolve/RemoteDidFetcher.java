package io.github.ivir3zam.didwebvh.core.resolve;

interface RemoteDidFetcher {

    String fetchDidLog(String httpsUrl);

    String fetchWitnessProofs(String witnessUrl);
}
