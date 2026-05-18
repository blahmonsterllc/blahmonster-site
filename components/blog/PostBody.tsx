'use client';

import Image from 'next/image';

function isProbablyRemote(src: string) {
	return /^https?:\/\//i.test(src);
}

export function PostBody({ content }: { content: string }) {
	const blocks = content.trim() ? content.split(/\n\n+/) : [];

	return (
		<>
			{blocks.map((block, idx) => {
				const imageLine = block.match(/^IMAGE:(\S+)(?:\s+([\s\S]*))?$/);
				if (imageLine) {
					const src = imageLine[1];
					const alt = (imageLine[2] || '').trim();
					const remote = isProbablyRemote(src);
					return (
						<figure key={idx} className="post-media">
							{remote ? (
								// eslint-disable-next-line @next/next/no-img-element -- remote user uploads / arbitrary URLs
								<img src={src} alt={alt} />
							) : (
								<Image
									src={src}
									alt={alt || 'Post image'}
									width={1400}
									height={933}
									sizes="(max-width: 900px) 100vw, 900px"
									style={{ width: '100%', height: 'auto' }}
								/>
							)}
							{alt ? <figcaption className="post-media-caption">{alt}</figcaption> : null}
						</figure>
					);
				}

				const videoLine = block.match(/^VIDEO:(\S+)\s*$/);
				if (videoLine) {
					return (
						<figure key={idx} className="post-media">
							<video controls playsInline src={videoLine[1]} preload="metadata" />
						</figure>
					);
				}

				return (
					<p key={idx} className="prose-para">
						{block}
					</p>
				);
			})}
		</>
	);
}
