export class RequestBuilder {
	private url: URL;
	private init: Omit<RequestInit, "headers"> & {
		headers: Headers;
	} = {
		method: "GET",
		headers: new Headers(),
	};
	private checkOk: boolean = false;

	private constructor(url: string | URL) {
		this.url = new URL(url);
	}

	static create(url: string | URL) {
		return new RequestBuilder(url);
	}

	query(key: string, value?: string) {
		if (value) this.url.searchParams.set(key, value);
		return this;
	}

	header(key: string, value: string) {
		this.init.headers.set(key, value);
		return this;
	}

	basicAuth(username: string, password: string) {
		const authHeader = `Basic ${btoa(`${username}:${password}`)}`;
		this.header("Authorization", authHeader);
		return this;
	}

	body(content: BodyInit) {
		this.init.body = content;
		return this;
	}

	method(method: string) {
		this.init.method = method;
		return this;
	}

	json(data: any) {
		this.header("Content-Type", "application/json");
		this.init.body = JSON.stringify(data);
		return this;
	}

	signal(signal: AbortSignal) {
		this.init.signal = signal;
		return this;
	}

	throwIfNotOk() {
		this.checkOk = true;
		return this;
	}

	build = (): RequestInit => this.init;

	async send(): Promise<Response> {
		const res = await fetch(this.url, this.build());
		if (this.checkOk && !res.ok) throw new Error(`Request failed with status ${res.status}`);
		return res;
	}
}
