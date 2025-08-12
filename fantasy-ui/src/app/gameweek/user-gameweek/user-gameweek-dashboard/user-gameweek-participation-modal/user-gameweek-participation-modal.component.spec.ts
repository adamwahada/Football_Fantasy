import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserGameweekParticipationModalComponent } from './user-gameweek-participation-modal.component';

describe('UserGameweekParticipationModalComponent', () => {
  let component: UserGameweekParticipationModalComponent;
  let fixture: ComponentFixture<UserGameweekParticipationModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserGameweekParticipationModalComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserGameweekParticipationModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
