import tailwindcssAnimate from 'tailwindcss-animate'

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,ts}'],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: '#2563EB',
          light: '#3B82F6',
          dark: '#1D4ED8'
        },
        ink: {
          DEFAULT: '#1F2937',
          soft: '#6B7280'
        }
      },
      fontFamily: {
        sans: ['PingFang SC', 'Microsoft YaHei', 'system-ui', 'sans-serif']
      },
      boxShadow: {
        soft: '0 10px 30px -12px rgba(37, 99, 235, 0.25)',
        card: '0 8px 24px -10px rgba(15, 23, 42, 0.12)'
      },
      keyframes: {
        floaty: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-6px)' }
        },
        blink: {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0' }
        }
      },
      animation: {
        floaty: 'floaty 6s ease-in-out infinite',
        blink: 'blink 1s step-end infinite'
      }
    }
  },
  plugins: [tailwindcssAnimate]
}
