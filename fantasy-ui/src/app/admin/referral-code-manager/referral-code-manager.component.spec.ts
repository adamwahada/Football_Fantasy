import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReferralCodeManagerComponent } from './referral-code-manager.component';

describe('ReferralCodeManagerComponent', () => {
  let component: ReferralCodeManagerComponent;
  let fixture: ComponentFixture<ReferralCodeManagerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReferralCodeManagerComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReferralCodeManagerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
