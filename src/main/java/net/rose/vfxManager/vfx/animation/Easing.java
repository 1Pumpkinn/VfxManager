package net.rose.vfxManager.vfx.animation;

public enum Easing {
    LINEAR {
        @Override
        public double curve(final double progress) {
            return progress;
        }
    },
    EASE_IN {
        @Override
        public double curve(final double progress) {
            return progress * progress * progress;
        }
    },
    EASE_OUT {
        @Override
        public double curve(final double progress) {
            final double inverse = 1.0D - progress;
            return 1.0D - (inverse * inverse * inverse);
        }
    },
    EASE_IN_OUT {
        @Override
        public double curve(final double progress) {
            if (progress < 0.5D) {
                return 4.0D * progress * progress * progress;
            }

            final double inverse = (-2.0D * progress) + 2.0D;
            return 1.0D - ((inverse * inverse * inverse) / 2.0D);
        }
    },
    SINE_IN {
        @Override
        public double curve(final double progress) {
            return 1.0D - Math.cos((progress * Math.PI) / 2.0D);
        }
    },
    SINE_OUT {
        @Override
        public double curve(final double progress) {
            return Math.sin((progress * Math.PI) / 2.0D);
        }
    },
    SINE_IN_OUT {
        @Override
        public double curve(final double progress) {
            return -(Math.cos(Math.PI * progress) - 1.0D) / 2.0D;
        }
    },
    BACK_OUT {
        @Override
        public double curve(final double progress) {
            final double c1 = 1.70158D;
            final double c3 = c1 + 1.0D;
            final double shifted = progress - 1.0D;
            return 1.0D + (c3 * shifted * shifted * shifted) + (c1 * shifted * shifted);
        }
    },
    ELASTIC_OUT {
        @Override
        public double curve(final double progress) {
            if (progress == 0.0D || progress == 1.0D) {
                return progress;
            }

            final double c4 = (2.0D * Math.PI) / 3.0D;
            return Math.pow(2.0D, -10.0D * progress)
                    * Math.sin(((progress * 10.0D) - 0.75D) * c4)
                    + 1.0D;
        }
    };

    public abstract double curve(double progress);

    public double apply(final double progress, final double start, final double end) {
        final double curved = curve(Math.clamp(progress, 0.0D, 1.0D));
        return start + ((end - start) * curved);
    }
}
