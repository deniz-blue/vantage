import { Credentials } from "../creds";

export const fetchAuth = async (url: string | URL, opts: RequestInit = {}): Promise<Response> => {
	const res = await fetch(url, opts);
	
	if (res.status === 401) {
		const wwwAuth = res.headers.get("WWW-Authenticate");
		if (wwwAuth) {
			const [scheme, ...params] = wwwAuth.split(" ");
			if (scheme!.toLowerCase() === "basic") {
				const creds = await Credentials.getAll();
				for (const cred of creds) {
					if (cred.type === "basic" && url.toString().startsWith(cred.url)) {
						const authHeader = `Basic ${btoa(`${cred.username}:${cred.password}`)}`;
						const authedRes = await fetch(url, {
							...opts,
							headers: {
								...opts.headers,
								Authorization: authHeader,
							},
						});
						if (authedRes.ok) return authedRes;
					}
				}
			}
		}
	}

	return res;
};
