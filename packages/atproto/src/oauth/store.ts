import type { Store } from "@atcute/oauth-node-client";

const PREFIX = "vantage:atproto:";

/**
 * Minimal async key-value storage interface.
 *
 * Accepts AsyncStorage (default), or any compatible implementation.
 */
export interface KVStorage {
	getItem(key: string): Promise<string | null>;
	setItem(key: string, value: string): Promise<void>;
	removeItem(key: string): Promise<void>;
	getAllKeys(): Promise<readonly string[]>;
	multiRemove(keys: readonly string[]): Promise<void>;
}

/**
 * Async key-value store implementing atcute's Store<K, V> interface.
 *
 * Values are JSON-serialized. Each namespace gets its own key prefix to avoid collisions.
 */
export class KVStore<V> implements Store<string, V> {
	private prefix: string;
	private storage: KVStorage;

	constructor(namespace: string, storage: KVStorage) {
		this.prefix = `${PREFIX}${namespace}:`;
		this.storage = storage;
	}

	async get(key: string): Promise<V | undefined> {
		const raw = await this.storage.getItem(this.prefix + key);
		return raw ? (JSON.parse(raw) as V) : undefined;
	}

	async set(key: string, value: V): Promise<void> {
		await this.storage.setItem(this.prefix + key, JSON.stringify(value));
	}

	async delete(key: string): Promise<void> {
		await this.storage.removeItem(this.prefix + key);
	}

	async clear(): Promise<void> {
		const keys = await this.storage.getAllKeys();
		const prefixed = keys.filter((k) => k.startsWith(this.prefix));
		if (prefixed.length) await this.storage.multiRemove(prefixed);
	}

	async keys(): Promise<string[]> {
		const all = await this.storage.getAllKeys();
		return all.filter((k) => k.startsWith(this.prefix)).map((k) => k.slice(this.prefix.length));
	}
}
