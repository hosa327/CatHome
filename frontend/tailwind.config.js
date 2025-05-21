/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{js,jsx,ts,tsx}"],
  theme: {
    extend: {
      keyframes: {
        wave: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-5%)' },
        },
      },
      animation: {
        wave: 'wave 3s ease-in-out infinite',
      },
    },
  },
  plugins: [],
}

