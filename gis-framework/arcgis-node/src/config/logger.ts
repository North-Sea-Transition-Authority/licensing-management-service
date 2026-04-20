import pino from 'pino';

const isProduction = process.env.NODE_ENV === 'production';
const hasPinoPretty = (() => {
  try {
    require.resolve('pino-pretty');
    return true;
  } catch {
    return false;
  }
})();

export const logger = pino({
  level: process.env.LOG_LEVEL || 'info',

  transport:
    !isProduction && hasPinoPretty
      ? {
          target: 'pino-pretty',
          options: {
            colorize: true,
            translateTime: 'SYS:standard',
            ignore: 'pid,hostname',
          },
        }
      : undefined,
});
