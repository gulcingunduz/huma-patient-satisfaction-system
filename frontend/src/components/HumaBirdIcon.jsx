export function HumaBirdIcon({ size = 22, className = "" }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 40 40"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-hidden="true"
    >
      {/* Graceful soaring Hüma bird silhouette with crest & flowing tail plumes */}
      <path
        d="M32.5 11.2C29.4 11.5 26.8 13.1 25.1 15.3C23.5 11.8 19.6 8.3 13.5 6.8C15.9 10.2 16.9 13.4 16.5 16.5C12.8 14.3 8.8 13.8 5.2 14.6C8.7 16.9 11.1 19.7 12.2 22.8C9.8 23.8 7.6 25.7 6.2 28.4C9.5 27.6 12.8 27.5 15.8 28.2C14.3 30.2 12.1 32 9.4 33.4C14.7 33.6 19.8 31.8 23.5 28.2C26.4 25.4 28.1 21.8 28.7 17.8C30.4 16.2 32.4 15.1 34.8 14.7C33.4 13.7 32.8 12.5 32.5 11.2Z"
        fill="currentColor"
      />
      {/* Hüma crown crest plume */}
      <path
        d="M27.6 10.6C28.8 8.3 31 6.7 33.6 6.2C32.1 7.9 31.2 9.6 30.8 11.2"
        stroke="#FCD34D"
        strokeWidth="2.1"
        strokeLinecap="round"
      />
      {/* Subtle wing highlight */}
      <path
        d="M17.2 18.6C20.2 17.8 23.1 18.4 25.3 20.1"
        stroke="#FCD34D"
        strokeWidth="1.6"
        strokeLinecap="round"
        opacity="0.9"
      />
    </svg>
  );
}
