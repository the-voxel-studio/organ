import { Component, Input, ElementRef, ViewChild, AfterViewInit, OnDestroy, HostBinding } from '@angular/core';
import { CommonModule } from '@angular/common';
import { gsap } from 'gsap';
import { MorphSVGPlugin } from 'gsap/MorphSVGPlugin';

@Component({
  selector: 'app-dynamic-logo',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dynamic-logo.html',
  styleUrl: './dynamic-logo.css'
})
export class DynamicLogoComponent implements AfterViewInit, OnDestroy {
  @Input() size: string = '48px';

  @HostBinding('style.width') get hostWidth() { return this.size; }
  @HostBinding('style.height') get hostHeight() { return this.size; }

  @ViewChild('logoSvg') logoSvg!: ElementRef<SVGElement>;
  @ViewChild('activeShape') activeShape!: ElementRef<SVGPathElement>;
  @ViewChild('circleRef') circleRef!: ElementRef<SVGCircleElement>;
  @ViewChild('heartRef') heartRef!: ElementRef<SVGPathElement>;
  @ViewChild('lungsRef') lungsRef!: ElementRef<SVGPathElement>;
  @ViewChild('brainRef') brainRef!: ElementRef<SVGPathElement>;
  @ViewChild('stomachRef') stomachRef!: ElementRef<SVGPathElement>;

  private timeline?: gsap.core.Timeline;
  private floatTimeline?: gsap.core.Timeline;

  ngAfterViewInit() {
    try {
      if (MorphSVGPlugin) {
        gsap.registerPlugin(MorphSVGPlugin);
        MorphSVGPlugin.convertToPath("circle");
        this.initAnimation();
      } else {
        this.initFallbackAnimation();
      }
    } catch (e) {
      console.warn("GSAP MorphSVGPlugin could not be loaded or registered. Animations are disabled.", e);
      this.initFallbackAnimation();
    }
  }

  ngOnDestroy() {
    if (this.timeline) this.timeline.kill();
    if (this.floatTimeline) this.floatTimeline.kill();
  }

  private initAnimation() {
    try {
      const shape = this.activeShape.nativeElement;
      this.timeline = gsap.timeline({ repeat: -1 });

      this.timeline
        .to(shape, {
          duration: 2,
          morphSVG: this.brainRef.nativeElement,
          ease: "back.inOut(1.2)"
        }, "+=1")
        .to(shape, {
          duration: 2,
          morphSVG: this.lungsRef.nativeElement,
          ease: "back.inOut(1.2)"
        }, "+=1")
        .to(shape, {
          duration: 2,
          morphSVG: this.stomachRef.nativeElement,
          ease: "back.inOut(1.2)"
        }, "+=1")
        .to(shape, {
          duration: 2,
          morphSVG: this.heartRef.nativeElement,
          ease: "back.inOut(1.2)"
        }, "+=1")
        .to(shape, {
          duration: 2,
          morphSVG: this.circleRef.nativeElement,
          ease: "back.inOut(1.2)"
        }, "+=1");
    } catch (e) {
      console.warn("Timeline morph animation setup failed. Using fallback floating animation.", e);
      if (this.timeline) {
        this.timeline.kill();
        this.timeline = undefined;
      }
    }

    this.initFallbackAnimation();
  }

  private initFallbackAnimation() {
    try {
      this.floatTimeline = gsap.timeline({ repeat: -1, yoyo: true });
      this.floatTimeline.to(this.logoSvg.nativeElement, {
        scale: 1.05,
        duration: 1.5,
        ease: "sine.inOut"
      });
    } catch (e) {
      console.error("GSAP float animation error:", e);
    }
  }
}
