export default function LogoMark({ size = 28, className = "" }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 32 32"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-hidden="true"
    >
      {/* Tag body - rounded rectangle rotated 45deg like a price tag */}
      <path
        d="M3 14.5L14.5 3H24C25.657 3 27 4.343 27 6V16.5L15.5 28C14.72 28.78 13.464 28.78 12.684 28L3.5 18.816C2.72 18.036 2.72 16.78 3 16.5V14.5Z"
        fill="#7C5CFF"
      />
      {/* Wing sweep - curves up and right from the tag tip */}
      <path
        d="M22 6C24 4 28.5 3.5 30 5C28 7 25 10 24 14C23.5 11 23 8.5 22 6Z"
        fill="#A998FF"
        opacity="0.85"
      />
      {/* Tag hole - circle cutout near top-left */}
      <circle cx="20.5" cy="10.5" r="2" fill="#131A2A" />
    </svg>
  );
}
