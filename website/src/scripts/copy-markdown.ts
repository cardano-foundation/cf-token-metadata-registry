// HTTP previews do not expose navigator.clipboard. Keep this fallback synchronous
// within the click; awaiting a fetch first loses the user gesture in WebKit.
const copyWithSelection = (text: string) => {
	const active = document.activeElement;
	const selection = document.getSelection();
	const ranges = Array.from({ length: selection?.rangeCount ?? 0 }, (_, i) => selection!.getRangeAt(i).cloneRange());
	const area = document.createElement('textarea');
	area.value = text;
	area.readOnly = true;
	area.tabIndex = -1;
	area.style.cssText = 'position:fixed;top:0;left:0;width:1px;height:1px;padding:0;border:0;opacity:0;font-size:16px';
	document.body.append(area);
	try {
		area.focus({ preventScroll: true });
		area.select();
		area.setSelectionRange(0, text.length);
		return document.execCommand('copy');
	} finally {
		area.remove();
		if (active instanceof HTMLElement && active.isConnected) active.focus({ preventScroll: true });
		selection?.removeAllRanges();
		for (const range of ranges) selection?.addRange(range);
	}
};

const setupCopy = (button: HTMLButtonElement) => {
	const label = button.querySelector<HTMLElement>('[data-copy-label]')!;
	let markdown: string | undefined;
	let reset: ReturnType<typeof setTimeout> | undefined;

	const prepare = async () => {
		button.disabled = true;
		button.setAttribute('aria-busy', 'true');
		label.textContent = 'Loading Markdown…';
		try {
			const response = await fetch(button.dataset.copyMarkdown!, { signal: AbortSignal.timeout(10000) });
			if (!response.ok || response.headers.get('content-type')?.includes('text/html')) {
				throw new Error(`Markdown unavailable: ${response.status}`);
			}
			markdown = await response.text();
			label.textContent = 'Copy as Markdown';
		} catch {
			label.textContent = 'Retry loading Markdown';
		} finally {
			button.disabled = false;
			button.removeAttribute('aria-busy');
		}
	};

	button.addEventListener('click', async () => {
		if (button.disabled) return;
		if (markdown === undefined) {
			await prepare();
			return;
		}
		clearTimeout(reset);
		button.disabled = true;
		button.setAttribute('aria-busy', 'true');
		label.textContent = 'Copying…';
		try {
			// Both paths begin during the click, with the content already available.
			if (navigator.clipboard?.writeText) await navigator.clipboard.writeText(markdown);
			else if (!copyWithSelection(markdown)) throw new Error('Copy command was refused');
			label.textContent = 'Copied';
		} catch {
			label.textContent = 'Copy failed';
		} finally {
			button.disabled = false;
			button.removeAttribute('aria-busy');
			reset = setTimeout(() => { label.textContent = 'Copy as Markdown'; }, 1600);
		}
	});

	void prepare();
};

for (const button of document.querySelectorAll<HTMLButtonElement>('[data-copy-markdown]')) setupCopy(button);
